package com.dusan.backend.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import com.dusan.backend.model.User;
import com.dusan.backend.repositories.UserRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {

    private PasswordEncoder passwordEncoder;

    private UserRepository userRepository;
    private TaskScheduler taskScheduler;

    @Autowired
    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository, TaskScheduler taskScheduler) {
        this.passwordEncoder = passwordEncoder;

        this.userRepository = userRepository;
        this.taskScheduler = taskScheduler;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User myUser = this.findByUsername(username);
        if(myUser == null) {
            throw new UsernameNotFoundException("User name "+username+" not found");
        }

        List<SimpleGrantedAuthority> authorities = myUser.getPermissions().stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
        return new org.springframework.security.core.userdetails.User(myUser.getUsername(), myUser.getPassword(), authorities);
    }

    public User create(User user) {
        if (this.userRepository.findByUsername(user.getUsername()) != null) {
//            throw new RuntimeException("A user with this username already exists.");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "A user with this username already exists."
            );
        }

        user.setPassword(this.passwordEncoder.encode(user.getPassword()));
        if(user.getIsAdmin() == null) user.setIsAdmin(false);
        return this.userRepository.save(user);
    }

    public Page<User> paginate(Integer page, Integer size) {
        return this.userRepository.findAll(PageRequest.of(page, size, Sort.by("salary").descending()));
    }

    public User findByUsername(String username) {
        //System.out.println("OVO NIJE NASAO: " + username);
        return this.userRepository.findByUsername(username);
    }

    public void loggedIn(String username) {
        User user = this.userRepository.findByUsername(username);
        Integer loginCount = user.getLoginCount();
        try {
//            Thread.sleep(10000);

            user.setLoginCount(loginCount + 1);
            this.userRepository.save(user);

        } catch (ObjectOptimisticLockingFailureException exception) {
            this.loggedIn(username);
        }
    }

    public User updateUser(Long id, User user) {

        Optional<User> existingUserOptional = userRepository.findById(id);

        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();

            existingUser.setUsername(user.getUsername());
            existingUser.setPassword(this.passwordEncoder.encode(user.getPassword()));
            existingUser.setPermissions(user.getPermissions());
            existingUser.setName(user.getName());
            return userRepository.save(existingUser);
        } else {
            throw new RuntimeException("User not found with ID: " + id);
        }
    }

    public void deleteUser(Long id) {

        if (userRepository.existsById(id)) {

            userRepository.deleteById(id);
        } else {
            throw new RuntimeException("User not found with ID: " + id);
        }
    }

//    @Scheduled(fixedDelay = 1000)
//    public void scheduleFixedDelayTask() throws InterruptedException {
//        System.out.println(
//                "Fixed delay task - " + System.currentTimeMillis() / 1000);
//        Thread.sleep(2000);
//    }


//    @Scheduled(fixedRate = 3000)
//    public void scheduleFixedRateTaskAsync() throws InterruptedException {
//        System.out.println(
//                "Fixed rate task async - " + System.currentTimeMillis() / 1000);
//        Thread.sleep(5000);
//        System.out.println(
//                "Fixed rate task async - finished " + System.currentTimeMillis() / 1000);
//    }

//    @Scheduled(cron = "0 * * * * *", zone = "Europe/Belgrade")
//    public void increaseUserBalance() {
//        System.out.println("Increasing balance...");
//        this.userRepository.increaseBalance(1);
//    }

    public User hire(String username, Integer salary) {
        User user = this.userRepository.findByUsername(username);
        user.setSalary(salary);
        this.userRepository.save(user);

        CronTrigger cronTrigger = new CronTrigger("0 * * * * *"); // "0 0 0 25 * *"
        this.taskScheduler.schedule(() -> {
            System.out.println("Getting salary...");
            this.userRepository.increaseBalance(salary);
        }, cronTrigger);

        return user;
    }

    public Optional<User> findById(Long id) {
        System.out.println("usao u find id");
        return userRepository.findById(id);
    }
}
