package com.dusan.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.dusan.backend.enums.DeliveryStatus;
import com.dusan.backend.model.Delivery;
import com.dusan.backend.model.Dish;
import com.dusan.backend.model.ErrorMessage;
import com.dusan.backend.model.User;
import com.dusan.backend.model.dtos.DeliveryDto;
import com.dusan.backend.model.dtos.DeliveryResponseDto;
import com.dusan.backend.model.dtos.ErrorMessageResponseDto;
import com.dusan.backend.model.helper.ScheduledJob;
import com.dusan.backend.model.helper.TrackedDelivery;
import com.dusan.backend.repositories.DeliveryRepository;
import com.dusan.backend.repositories.ErrorMessageRepository;
import com.dusan.backend.repositories.UserRepository;
import com.dusan.backend.webSockets.OrderStatusMessage;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

@Service
public class DeliveryService {

    private final DeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final TaskScheduler taskScheduler;
    private final ErrorMessageRepository errorMessageRepository;

    private final ConcurrentHashMap<Long, ReentrantLock> deliveryLocks = new ConcurrentHashMap<>();

    //max active menadzerisanje
    private static final int MAX_ACTIVE = 3;
    private final ConcurrentHashMap<Long, TrackedDelivery> trackedDeliveries = new ConcurrentHashMap<>();
    private final Random rand = new Random();

    private final ReentrantLock capacityLock = new ReentrantLock();

    //zakazivanje
    private final ConcurrentHashMap<Integer, ScheduledJob> scheduledJobs = new ConcurrentHashMap<>();
    private final AtomicInteger scheduledJobSeq = new AtomicInteger(1);

    //poruke
//    private final SimpMessagingTemplate messagingTemplate;
    private final SimpMessageSendingOperations messagingTemplate;

    @Autowired
    public DeliveryService(DeliveryRepository deliveryRepository,
                           UserRepository userRepository,
                           TaskScheduler taskScheduler,
                           ErrorMessageRepository errorMessageRepository,
                           SimpMessageSendingOperations messagingTemplate) {
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
        this.taskScheduler = taskScheduler;
        this.errorMessageRepository = errorMessageRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<DeliveryResponseDto> search(String username, List<String> statuses, String dateFrom, String dateTo, Long userId){

        User user = userRepository.findByUsername(username);
        if(user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

        boolean isAdmin = user.getIsAdmin();

        List<Delivery> deliveries;
        //cije uzimam
        if(isAdmin && userId != null){
            deliveries = deliveryRepository.findByCreatedByUserId(userId);
        }else if(isAdmin){
            deliveries = deliveryRepository.findAll();
        }else{
            deliveries = deliveryRepository.findByCreatedByUserId(user.getUserId());
        }
        //pa filterisanje
        if(statuses != null && !statuses.isEmpty()){
            deliveries = deliveries.stream()
                    .filter(d -> statuses.contains(d.getStatus().name()))
                    .collect(Collectors.toList());
        }

        if(dateFrom != null && dateTo != null){
            try{
                LocalDateTime from = LocalDateTime.parse(dateFrom);
                LocalDateTime to = LocalDateTime.parse(dateTo);
                deliveries = deliveries.stream()
                        .filter(d -> d.getCreatedAt().isAfter(from) && d.getCreatedAt().isBefore(to))
                        .collect(Collectors.toList());
            }catch(Exception e){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid date format");
            }
        }
        return deliveries.stream()
                .map(DeliveryResponseDto::new)
                .collect(Collectors.toList());
    }

    public DeliveryResponseDto placeOrder(DeliveryDto deliveryDto, String username) {

        User user = userRepository.findByUsername(username);
        if(user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

        Delivery delivery = new Delivery();
        delivery.setStatus(DeliveryStatus.ORDERED);
        delivery.setCreatedBy(user);
        delivery.setActive(true);
        delivery.setCreatedAt(deliveryDto.getScheduledAt() != null ? deliveryDto.getScheduledAt() : LocalDateTime.now());

        List<Dish> dishes = deliveryDto.getDishes().stream()
                .map(s -> {
                    String[] parts = s.split(",");
                    if(parts.length != 3){
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dish format");
                    }
                    return new Dish(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                }).collect(Collectors.toList());

        delivery.setItems(dishes);
        delivery = deliveryRepository.save(delivery);

        //kapacitet racun
        int baseDelaySeconds = 10;
        int randomDeviation = rand.nextInt(5);
        int totalDelay = baseDelaySeconds + randomDeviation;
        LocalDateTime expectedPreparingTime = LocalDateTime.now().plusSeconds(totalDelay);

        //provere i dodavanje conc
        capacityLock.lock();
        try {
            long activeCount = trackedDeliveries.values().stream()
                    .filter(t ->
                            (t.getStatus() == DeliveryStatus.IN_DELIVERY && t.getExpectedNextChange().isAfter(expectedPreparingTime.minusNanos(1)))
                                    || (t.getStatus() == DeliveryStatus.ORDERED)
                                    || t.getStatus() == DeliveryStatus.PREPARING
                    ).count();

            //provera za scheduledJobs

            //-60 za overlap u nazad
            LocalDateTime windowStart = expectedPreparingTime.minusSeconds(60);
            // maks od pripreme 50
            LocalDateTime windowEnd = expectedPreparingTime.plusSeconds(50);
            long scheduledOverlaps = scheduledJobs.values().stream()
                    .filter(sj -> !sj.getScheduledAt().isBefore(windowStart) && !sj.getScheduledAt().isAfter(windowEnd))
                    .count();

            long entireCount = activeCount + scheduledOverlaps;

            if (entireCount >= MAX_ACTIVE) {
                //upis u greske / declined
                ErrorMessage error = new ErrorMessage();

                error.setUser(user);
                error.setCreatedAt(LocalDateTime.now());
                error.setDelivery(delivery);
                error.setStatusAtDecline(delivery.getStatus());
                error.setMessage("Order declined due to too many active deliveries");
                errorMessageRepository.save(error);

                delivery.setStatus(DeliveryStatus.DECLINED); //jer sto da ne
                deliveryRepository.save(delivery);

                throw new ResponseStatusException(HttpStatus.CONFLICT, "To many deliveries at this time \n Declined when ORDERED :(");
            }
            //pa ako je prihvatim
            trackedDeliveries.put(delivery.getId(), new TrackedDelivery(
                    delivery.getId(),
                    DeliveryStatus.ORDERED,
                    expectedPreparingTime
            ));

            scheduleStatusChange(delivery.getId(), DeliveryStatus.ORDERED, totalDelay);
            //i slanje
            sendOrderUpdateToOwner(delivery);

        }finally {
            capacityLock.unlock();
        }
        return new DeliveryResponseDto(delivery);
    }

    public DeliveryResponseDto cancelOrder(Long id, String username){

        ReentrantLock lock = deliveryLocks.computeIfAbsent(id, k -> new ReentrantLock());
        lock.lock();
        try{
            Optional<Delivery> optionalDelivery = deliveryRepository.findById(id);
            if(!optionalDelivery.isPresent()){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery not found");
            }
            Delivery delivery = optionalDelivery.get();

            User user = userRepository.findByUsername(username);
            if(user == null){
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
            }
            boolean isAdmin = user.getIsAdmin();
            if(!isAdmin && !delivery.getCreatedBy().getUserId().equals(user.getUserId())){
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "User not allowed to cancel this delivery");
            }

            //i sad kad konacno moze hahah i to mozda
            if(delivery.getStatus() == DeliveryStatus.ORDERED){

                delivery.setStatus(DeliveryStatus.CANCELED);
                delivery.setActive(false);
                delivery = deliveryRepository.save(delivery);

                updateTrackedDelivery(delivery.getId(), delivery.getStatus(), LocalDateTime.now()); //da ga odmah makne

                sendOrderUpdateToOwner(delivery);

                return new DeliveryResponseDto(delivery);
            }else{
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Don't be mean and try to cancel a on-your-way delivery");
            }
        }finally {
            lock.unlock();
        }



    }

    public DeliveryResponseDto trackOrder(Long id, String username){
        //TODO  T

        return null;
    }

    public DeliveryResponseDto scheduleOrder(DeliveryDto deliveryDto, String username) {

        if(deliveryDto.getScheduledAt() == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid schedule date");
        User user = userRepository.findByUsername(username);
        if(user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

        List<Dish> dishes = deliveryDto.getDishes().stream()
                .map(s -> {
                    String[] parts = s.split(",");
                    if(parts.length != 3){
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid dish format");
                    }
                    return new Dish(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
                }).collect(Collectors.toList());

        LocalDateTime scheduledAt = deliveryDto.getScheduledAt();

        int baseFirstDelay = 10;
        int maxDeviationForCheck = 5;
        int estimatedFirstDelay = baseFirstDelay + maxDeviationForCheck;
        LocalDateTime estimatedPreparingTime = scheduledAt.plusSeconds(estimatedFirstDelay); //vreme do prep

        int jobId = scheduledJobSeq.getAndIncrement();
        Delivery delivery = new Delivery();
        delivery.setStatus(DeliveryStatus.SCHEDULED);
        delivery.setCreatedBy(user);
        delivery.setActive(true);
        delivery.setCreatedAt(deliveryDto.getScheduledAt() != null ? deliveryDto.getScheduledAt() : LocalDateTime.now());


        delivery.setItems(dishes);
        delivery = deliveryRepository.save(delivery);

        long deliveryId = delivery.getId();
        ScheduledJob job = new ScheduledJob(
                jobId,
                scheduledAt,
                estimatedPreparingTime,
                estimatedFirstDelay,
                deliveryId
        );

        capacityLock.lock();
        try{
            //TODO PROVERITI JOS JEDNOM LOGIKU RACUNANJA VREMENA
            //od pocetka max je 60s pa minus estimated max order->prep vreme
            LocalDateTime windowStart = estimatedPreparingTime.minusSeconds(60);
            //min ord vreme + max prep+del vreme 10+20+25
            LocalDateTime windowEnd = estimatedPreparingTime.plusSeconds(55);


            long scheduledOverlaps = scheduledJobs.values().stream()
                    .filter(sj -> !sj.getScheduledAt().isBefore(windowStart) && !sj.getScheduledAt().isAfter(windowEnd))
                    .count();

            //za postojece
            LocalDateTime trackedCutoff = scheduledAt;

            long trackedOverlaps = trackedDeliveries.values().stream()
                    .filter(td -> {
                        if(td.getStatus() == DeliveryStatus.ORDERED || td.getStatus() == DeliveryStatus.PREPARING){
                            return Math.abs(Duration.between(td.getExpectedNextChange(), trackedCutoff).getSeconds()) <= 45;
                        }else if(td.getStatus() == DeliveryStatus.IN_DELIVERY){
                            return Math.abs(Duration.between(td.getExpectedNextChange(), trackedCutoff).getSeconds()) <= 25;
                        }
                        return false;
                    }).count();

            long totalActive = scheduledOverlaps + trackedOverlaps;
            if(totalActive >= MAX_ACTIVE){
                //upis u greske / declined
                ErrorMessage error = new ErrorMessage();
                error.setUser(user);
                error.setCreatedAt(LocalDateTime.now());
                error.setDelivery(null); //jer nikad nije ni napravljena
                error.setStatusAtDecline(null);
                error.setMessage("Scheduled order declined due to too many deliveries at desired time");
                errorMessageRepository.save(error);

                throw new ResponseStatusException(HttpStatus.CONFLICT, "Too many deliveries would be active at that time — scheduling declined");

            }
            //jej


            scheduledJobs.put(jobId, job);
            Instant runInstant = scheduledAt.atZone(ZoneId.systemDefault()).toInstant();
            taskScheduler.schedule(() -> executeScheduledJob(jobId, deliveryId), runInstant);

        }finally {
            capacityLock.unlock();
        }

        return null;
    }


    //pomocne
    private void scheduleStatusChange(Long deliveryId, DeliveryStatus currentStatus, int forOrdered) {
        int delaySeconds;

        switch(currentStatus){
            case ORDERED:
                delaySeconds = forOrdered;
                break;
            case PREPARING:
                delaySeconds = 15 + rand.nextInt(5);
                break;
            case IN_DELIVERY:
                delaySeconds = 20 + rand.nextInt(5);
                break;
            default:
                return;
        }

        taskScheduler.schedule(()->{
            ReentrantLock lock = deliveryLocks.computeIfAbsent(deliveryId, id -> new ReentrantLock());
            lock.lock();
            System.out.println("l o k");

            DeliveryStatus newStatus = DeliveryStatus.CANCELED;

            try{
                Optional<Delivery> optionalDelivery = deliveryRepository.findById(deliveryId);
                if (optionalDelivery.isPresent()) {
                    Delivery delivery = optionalDelivery.get();

                    if (delivery.getActive() && delivery.getStatus() != DeliveryStatus.CANCELED) {
//                        DeliveryStatus newStatus = null;
                        if (delivery.getStatus() == DeliveryStatus.ORDERED) {
                            newStatus = DeliveryStatus.PREPARING;
                        } else if (delivery.getStatus() == DeliveryStatus.PREPARING) {
                            newStatus = DeliveryStatus.IN_DELIVERY;
                        } else if (delivery.getStatus() == DeliveryStatus.IN_DELIVERY) {
                            newStatus = DeliveryStatus.DELIVERED;
                        }

                        if (newStatus != DeliveryStatus.CANCELED) {
                            delivery.setStatus(newStatus);
                            deliveryRepository.save(delivery);
                            System.out.println("Delivery " + deliveryId + "is now in " + newStatus + " state");

                            //slanje poruke
                            sendOrderUpdateToOwner(delivery);

                            //rek
                            scheduleStatusChange(deliveryId, newStatus, 0);
                        }
                    }else{
                        System.out.println("Delivery " + deliveryId + " is canceled :(");
                    }
                }
            }finally {

                LocalDateTime nextChange = LocalDateTime.now().plusSeconds(delaySeconds);
                updateTrackedDelivery(deliveryId, newStatus, nextChange);
                System.out.println("delivery " + deliveryId + " is now in " + newStatus + " state with \n " +
                        "expected change: " + nextChange);

                System.out.println("a n l");
                lock.unlock();
            }
        }, Instant.now().plusSeconds(delaySeconds));
    }

    private void updateTrackedDelivery(Long id, DeliveryStatus newStatus, LocalDateTime nextChange){
        if(newStatus == DeliveryStatus.DELIVERED || newStatus == DeliveryStatus.CANCELED){
            trackedDeliveries.remove(id);
        }else{
            trackedDeliveries.put(id, new TrackedDelivery(id, newStatus, nextChange));
        }
    }

    //zakazivanje job deo
    private void executeScheduledJob(int jobId, long deliveryId){
        ScheduledJob job = scheduledJobs.get(jobId);
        if(job == null){
            return;
        }
        capacityLock.lock();
        try{

            scheduledJobs.remove(jobId);
            Delivery delivery = deliveryRepository.findById(deliveryId).get();
            if(delivery == null)
                return;

            User user = delivery.getCreatedBy();
            if(user == null)
                return;


            int actualFirstDelay = 10 + rand.nextInt(5);
            LocalDateTime actualExpectedUntilPreparing = LocalDateTime.now().plusSeconds(actualFirstDelay);

            //ista provera kao i za place order
            long activeCount = trackedDeliveries.values().stream()
                    .filter(t ->
                            (t.getStatus() == DeliveryStatus.IN_DELIVERY && t.getExpectedNextChange().isAfter(actualExpectedUntilPreparing.minusNanos(1)))
                                    || (t.getStatus() == DeliveryStatus.ORDERED)
                                    || t.getStatus() == DeliveryStatus.PREPARING
                    ).count();

            //provera za scheduledJobs

            //-60 za overlap u nazad
            LocalDateTime windowStart = actualExpectedUntilPreparing.minusSeconds(60);
            // maks od pripreme 50
            LocalDateTime windowEnd = actualExpectedUntilPreparing.plusSeconds(50);
            long scheduledOverlaps = scheduledJobs.values().stream()
                    .filter(sj -> !sj.getScheduledAt().isBefore(windowStart) && !sj.getScheduledAt().isAfter(windowEnd))
                    .count();

            long entireCount = activeCount + scheduledOverlaps;

            if (entireCount >= MAX_ACTIVE) {
                //upis u greske / declined
                ErrorMessage error = new ErrorMessage();

                error.setUser(user);
                error.setCreatedAt(LocalDateTime.now());
                error.setDelivery(delivery);
                error.setStatusAtDecline(delivery.getStatus());
                error.setMessage("Order declined after it was scheduled due to too many active deliveries atm :(");
                errorMessageRepository.save(error);

                delivery.setStatus(DeliveryStatus.DECLINED); //jer sto da ne
                deliveryRepository.save(delivery);

                throw new ResponseStatusException(HttpStatus.CONFLICT, "To many deliveries at this time \n declined even though it was scheduled sorke :(");
            }

            trackedDeliveries.put(delivery.getId(), new TrackedDelivery(delivery.getId(), DeliveryStatus.ORDERED, actualExpectedUntilPreparing));

            scheduleStatusChange(delivery.getId(), DeliveryStatus.ORDERED, actualFirstDelay);

            //update za usera odmah
            sendOrderUpdateToOwner(delivery);

        }finally {
            capacityLock.unlock();
        }

    }

    //za err
    public Page<ErrorMessageResponseDto> searchErrors(String username, Long userId, Pageable pageable){
        User user = userRepository.findByUsername(username);
        if(user == null)
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");

        boolean isAdmin = user.getIsAdmin();
        Page<ErrorMessage> errors;
        if(isAdmin && userId != null){
            errors = errorMessageRepository.findByUser_UserId(userId, pageable);
        }else if(isAdmin){
            errors = errorMessageRepository.findAll(pageable);
        }else{
            errors = errorMessageRepository.findByUser_UserId(user.getUserId(), pageable);
        }
        return errors.map(ErrorMessageResponseDto::new);
    }

    private void sendOrderUpdateToOwner(Delivery delivery){
        //this.messagingTemplate.convertAndSend("/topic/messages", message);
        if(delivery == null)return;
//        String username = delivery.getCreatedBy().getUsername(); // mozda promeniti  ako vristi
        Optional<String> usernameOpt = deliveryRepository.findUsernameByDeliveryId(delivery.getId());
        String username;
        if(usernameOpt.isPresent()){
            username = usernameOpt.get();
        }else{
            System.out.println("No username found for delivery id " + delivery.getId() + " skipping WS send :(");
            return;
        }

        OrderStatusMessage msg = new OrderStatusMessage(
                delivery.getId(),
                delivery.getStatus(),
                username,
                LocalDateTime.now()
        );

        System.out.println("would send to: " + username + " and the message would be: " + msg);

        this.messagingTemplate.convertAndSend("/topic/messages", msg);
    }


}