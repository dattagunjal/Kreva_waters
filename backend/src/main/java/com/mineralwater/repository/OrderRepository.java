package com.mineralwater.repository;

import com.mineralwater.model.Order;
import com.mineralwater.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserOrderByCreatedAtDesc(User user);
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
