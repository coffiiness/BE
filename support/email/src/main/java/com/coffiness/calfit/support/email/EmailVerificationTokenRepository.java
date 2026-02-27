package com.coffiness.calfit.support.email;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface  EmailVerificationTokenRepository
    extends JpaRepository<EmailVerificationTokenEntity, Long> {

  Optional<EmailVerificationTokenEntity> findByToken(String token);

  Optional<EmailVerificationTokenEntity> findByUserEmail(String userEmail);
}
