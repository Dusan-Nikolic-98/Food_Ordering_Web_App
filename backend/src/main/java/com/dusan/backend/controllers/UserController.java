package com.dusan.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.dusan.backend.model.User;
import com.dusan.backend.services.UserService;

import javax.validation.Valid;

@RestController
@RequestMapping("/users")
@CrossOrigin
public class UserController {

    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PreAuthorize("hasAuthority('can_create_users')")
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public User create(@Valid @RequestBody User user) {
        return this.userService.create(user);
    }

    @PreAuthorize("hasAuthority('can_read_users')")
    @GetMapping
    public Page<User> all(@RequestParam(defaultValue = "0") Integer page, @RequestParam(defaultValue = "10") Integer size) {
        return this.userService.paginate(page, size);
    }

    @PreAuthorize("hasAuthority('can_update_users')")
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @Valid @RequestBody User user) {

        return userService.updateUser(id, user);
    }

    @PreAuthorize("hasAuthority('can_delete_users')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {

        userService.deleteUser(id);
    }

    @GetMapping(value = "/me", produces = MediaType.APPLICATION_JSON_VALUE)
    public User me() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return this.userService.findByUsername(username);
    }

    @PostMapping(value = "/hire", produces = MediaType.APPLICATION_JSON_VALUE)
    public User hire(@RequestParam("salary") Integer salary) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return this.userService.hire(username, salary);
    }

    @PreAuthorize("hasAuthority('can_read_users')")
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public User getUserById(@PathVariable Long id) {
        System.out.println("VALJDA JE NASAO PUTANJU SAD" + id);

        return userService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
