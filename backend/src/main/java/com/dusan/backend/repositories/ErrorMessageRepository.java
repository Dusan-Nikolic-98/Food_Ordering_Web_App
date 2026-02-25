package com.dusan.backend.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.dusan.backend.model.ErrorMessage;

public interface ErrorMessageRepository extends JpaRepository<ErrorMessage, Long> {
    Page<ErrorMessage> findByUser_UserId(Long userId, Pageable pageable);
}
