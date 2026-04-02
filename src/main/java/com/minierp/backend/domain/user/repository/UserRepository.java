package com.minierp.backend.domain.user.repository;

import com.minierp.backend.domain.user.entity.User;
import com.minierp.backend.domain.user.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    boolean existsByLoginId(String loginId);

    boolean existsByUserEmail(String userEmail);

    Optional<User> findByLoginId(String loginId);

    Optional<User> findByUserEmail(String userEmail);

    List<User> findByUserRole(UserRole userRole);
}
