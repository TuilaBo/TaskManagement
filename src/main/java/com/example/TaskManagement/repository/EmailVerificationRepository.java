package com.example.TaskManagement.repository;

import com.example.TaskManagement.model.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmailAndCode(String email, String code);

    void deleteByEmail(String email);

    boolean existsByEmail(String email);
}
