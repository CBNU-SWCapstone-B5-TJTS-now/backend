package com.nowhere.backend.repository;

import com.nowhere.backend.domain.entity.PointTransaction;
import com.nowhere.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointTransactionRepository extends JpaRepository<PointTransaction, Long> {

    List<PointTransaction> findByUserOrderByCreatedAtDesc(User user);
}
