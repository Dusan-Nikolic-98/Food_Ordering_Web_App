package com.dusan.backend.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.dusan.backend.model.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    public User findByUsername(String username);


    @Modifying
    @Query("update User u set u.balance = u.balance + :amount")
    @Transactional
    public void increaseBalance(@Param("amount") Integer amount);

    @Query("update User u set u.loginCount = u.loginCount + :amount")
    @Modifying
    @Transactional
    public void increaseLogin(@Param("amount") Integer amount);
}
