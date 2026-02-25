package com.dusan.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.dusan.backend.model.Delivery;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryRepository extends JpaRepository<Delivery, Long> {
    List<Delivery> findByCreatedByUserId(Long userId);

    //jer u suprotnom u poruci 2.put dodje do pucanja za delivery.getCreatedBy().getUsername(),
    // LazyInitializationException puca tu
    @Query("select d.createdBy.username from Delivery d where d.id = :id")
    Optional<String> findUsernameByDeliveryId(@Param("id") Long id);
}
