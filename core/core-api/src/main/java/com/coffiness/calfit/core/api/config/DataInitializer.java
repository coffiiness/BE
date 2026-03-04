package com.coffiness.calfit.core.api.config;

import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

  private static final String ADMIN_EMAIL = "admin@coffiiness.com";
  private static final String ADMIN_PASSWORD = "admin1234!";
  private static final String ADMIN_NAME = "Admin";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Override
  public void run(ApplicationArguments args) {
    if (userRepository.existsByEmail(ADMIN_EMAIL)) {
      return;
    }

    UserEntity admin =
        UserEntity.createAdmin(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), ADMIN_NAME);
    userRepository.save(admin);
    log.info("[DataInitializer] Admin account created: {}", ADMIN_EMAIL);
  }
}
