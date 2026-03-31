package com.minierp.backend.domain.user.repository;

import com.minierp.backend.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLoginId(String loginId);

    boolean existsByUserEmail(String userEmail);

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByUserEmail(String userEmail);
}
