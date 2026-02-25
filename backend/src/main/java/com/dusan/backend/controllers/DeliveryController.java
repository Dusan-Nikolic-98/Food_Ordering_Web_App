package com.dusan.backend.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.dusan.backend.model.Delivery;
import com.dusan.backend.model.Dish;
import com.dusan.backend.model.DishWithImg;
import com.dusan.backend.model.dtos.DeliveryDto;
import com.dusan.backend.model.dtos.DeliveryResponseDto;
import com.dusan.backend.model.dtos.ErrorMessageResponseDto;
import com.dusan.backend.services.DeliveryService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/deliveries")
@CrossOrigin
public class DeliveryController {
    private final DeliveryService deliveryService;

    private static final List<DishWithImg> PREDEFINED_DISHES = Arrays.asList(
            new DishWithImg("Pizza Margherita" ,850,"https://images.unsplash.com/photo-1513104890138-7c749659a591?w=400"),
            new DishWithImg("Spaghetti Carbonara", 680,"https://images.unsplash.com/photo-1633337474564-1d9478ca4e2e?w=400"),
            new DishWithImg("Caesar Salad", 480,"https://images.unsplash.com/photo-1512621776951-a57141f2eefd?w=400"),
            new DishWithImg("Grilled Salmon", 1250,"https://images.unsplash.com/photo-1519708227418-c8fd9a32b7a2?w=400"),
            new DishWithImg("Chocolate Lava Cake", 380,"https://images.unsplash.com/photo-1578985545062-69928b1d9587?w=400"),
            new DishWithImg("Beef Burger", 550,"https://images.unsplash.com/photo-1553979459-d2229ba7433b?w=400"),
            new DishWithImg("Chicken Tikka Masala", 720,"https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=400"),
            new DishWithImg("Vegetable Stir Fry", 420,"https://images.unsplash.com/photo-1540420773420-3366772f4999?w=400"),
            new DishWithImg("Tiramisu", 320,"https://images.unsplash.com/photo-1571877227200-a0d98ea607e9?w=400"),
            new DishWithImg("Margarita Cocktail", 280,"https://images.unsplash.com/photo-1551538827-9c037cb4f32a?w=400")
    );

    @Autowired
    public DeliveryController(DeliveryService deliveryService) {
        this.deliveryService = deliveryService;
    }

    @PreAuthorize("hasAuthority('can_search_order')")
    @GetMapping
    public List<DeliveryResponseDto> search(
            @RequestParam(required = false) List<String> status,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo,
            @RequestParam(required = false) Long userId
    ){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return deliveryService.search(username, status, dateFrom, dateTo, userId);

    }

    @PreAuthorize("hasAuthority('can_place_order')")
    @PostMapping(value = "/place", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeliveryResponseDto placeOrder(@RequestBody DeliveryDto deliveryDto){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return deliveryService.placeOrder(deliveryDto, username);
    }

    @PreAuthorize("hasAuthority('can_cancel_order')")
    @PostMapping("/{id}/cancel")
    public DeliveryResponseDto cancelOrder(@PathVariable Long id){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return deliveryService.cancelOrder(id, username);
    }

    @PreAuthorize("hasAuthority('can_track_order')")
    @GetMapping("/{id}/track")
    public DeliveryResponseDto trackOrder(@PathVariable Long id){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return deliveryService.trackOrder(id, username);
    }

    @PreAuthorize("hasAuthority('can_schedule_order')")
    @PostMapping(value = "/schedule", produces = MediaType.APPLICATION_JSON_VALUE)
    public DeliveryResponseDto scheduleOrder(@RequestBody DeliveryDto deliveryDto){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return deliveryService.scheduleOrder(deliveryDto, username);
    }

    //za err
    @GetMapping("/errors")
    public Page<ErrorMessageResponseDto> searchErrors(
            @RequestParam(required = false) Long userId,
            @PageableDefault(page = 0, size = 10) Pageable pageable
    ){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return deliveryService.searchErrors(username, userId, pageable);

    }

    @GetMapping("/dishes")
    public List<DishWithImg> getDishes() {
        return PREDEFINED_DISHES;
    }

}
