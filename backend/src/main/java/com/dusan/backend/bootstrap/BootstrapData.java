package com.dusan.backend.bootstrap;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import com.dusan.backend.enums.DeliveryStatus;
import com.dusan.backend.model.*;
import com.dusan.backend.enums.Permissions;
import com.dusan.backend.repositories.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class BootstrapData implements CommandLineRunner {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;
    private final DeliveryRepository deliveryRepository;

    @Autowired
    public BootstrapData(UserRepository userRepository, PasswordEncoder passwordEncoder, DeliveryRepository deliveryRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.deliveryRepository = deliveryRepository;
    }

    @Override
    public void run(String... args) throws Exception {

        System.out.println("Maybe loading data...");

        if(userRepository.findByUsername("admin@admin.admin") == null) {
            User admin = new User();
            admin.setUsername("admin@admin.admin");
            admin.setPassword(this.passwordEncoder.encode("admin"));
            List<String> svePerm = Arrays.stream(Permissions.values())
                    .map(Enum::name)
                    .collect(Collectors.toList());
            admin.setPermissions(new HashSet<>(svePerm));
            admin.setName("admin");
            admin.setIsAdmin(true);
            this.userRepository.save(admin);

            User user1 = new User();
            user1.setUsername("user1@user.user");
            user1.setPassword(this.passwordEncoder.encode("user1"));
            user1.setPermissions(new HashSet<>(Arrays.asList(Permissions.can_read_users.toString(), Permissions.can_update_users.toString(),
                    Permissions.can_create_users.toString(), Permissions.can_delete_users.toString())));
            user1.setName("user1");
            this.userRepository.save(user1);

            User user2 = new User();
            user2.setUsername("user2@user.user");
            user2.setPassword(this.passwordEncoder.encode("user2"));
            user2.setPermissions(new HashSet<>(svePerm));
            user2.setName("user2");
            this.userRepository.save(user2);

            User user3 = new User();
            user3.setUsername("user3@user.user");
            user3.setPassword(this.passwordEncoder.encode("user3"));
            user3.setPermissions(new HashSet<>(Arrays.asList("can_read_users")));
            user3.setName("user3");
            this.userRepository.save(user3);

            System.out.println("Data loaded!");
        }else{
            System.out.println("Data already loaded!");
        }

        //deliveri deo dodavanje
        if(this.userRepository.findAll().isEmpty()) {
            User admin = this.userRepository.findByUsername("admin@admin.admin");
            User user1 = this.userRepository.findByUsername("user1@user.user");
            User user2 = this.userRepository.findByUsername("user2@user.user");
            User user3 = this.userRepository.findByUsername("user3@user.user");
            createTestDeliveries(admin, user1, user2, user3);
        }
    }

    private void createTestDeliveries(User admin, User user1, User user2, User user3) {
        LocalDateTime now = LocalDateTime.now();

        // adminove
        createDelivery(admin, DeliveryStatus.ORDERED, "Pizza,2,1200;Salad,1,500", now.minusDays(5), true);
        createDelivery(admin, DeliveryStatus.CANCELED, "Burger,3,800;Fries,2,400", now.minusHours(3), true);
        createDelivery(admin, DeliveryStatus.DELIVERED, "Sushi,1,1500;Miso Soup,1,300", now.minusDays(2), true);
        createDelivery(admin, DeliveryStatus.IN_DELIVERY, "Pasta,2,900;Garlic Bread,1,250", now.minusDays(1), false);

        // user1
        createDelivery(user1, DeliveryStatus.CANCELED, "Tacos,4,600;Guacamole,1,200", now.minusHours(1), true);
        createDelivery(user1, DeliveryStatus.ORDERED, "Steak,1,1800;Mashed Potatoes,1,350", now.minusMinutes(30), true);
        createDelivery(user1, DeliveryStatus.DELIVERED, "Chicken Wings,10,1200;Celery Sticks,1,100", now.minusDays(1), true);

        // user2
        createDelivery(user2, DeliveryStatus.ORDERED, "Ramen,2,950;Gyoza,6,450", now.minusHours(12), true);
        createDelivery(user2, DeliveryStatus.DELIVERED, "Sandwich,1,550;Chips,1,150", now.minusDays(3), true);
        createDelivery(user2, DeliveryStatus.ORDERED, "Ice Cream,2,350;Brownie,1,450", now.minusHours(6), false);
        createDelivery(user2, DeliveryStatus.ORDERED, "Curry,2,1100;Naan,2,200", now.minusHours(2), true);

        // user3
        createDelivery(user3, DeliveryStatus.ORDERED, "Soup,1,400;Bread,2,150", now.minusHours(4), true);
        createDelivery(user3, DeliveryStatus.DELIVERED, "Salad,1,450;Juice,1,200", now.minusDays(4), true);

        System.out.println("Created 14 test deliveries with various statuses and dates!");
    }


    private void createDelivery(User user, DeliveryStatus status, String dishes, LocalDateTime createdAt, boolean active) {
        Delivery delivery = new Delivery();
        delivery.setCreatedBy(user);
        delivery.setStatus(status);
        delivery.setDishesString(dishes);
        delivery.setCreatedAt(createdAt);
        delivery.setActive(active);
        deliveryRepository.save(delivery);
    }
}
