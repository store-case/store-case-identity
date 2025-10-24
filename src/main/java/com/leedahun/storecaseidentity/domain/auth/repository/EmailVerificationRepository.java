package com.leedahun.storecaseidentity.domain.auth.repository;

import com.leedahun.storecaseidentity.domain.auth.entity.EmailPurpose;
import com.leedahun.storecaseidentity.domain.auth.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findByEmail(String email);

    Optional<EmailVerification> findByEmailAndPurpose(String email, EmailPurpose purpose);

    Optional<EmailVerification> findTopByEmailAndPurposeOrderByIdDesc(String email, EmailPurpose purpose);

}
