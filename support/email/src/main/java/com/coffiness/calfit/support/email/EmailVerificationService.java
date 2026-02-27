package com.coffiness.calfit.support.email;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

  private final EmailVerificationTokenRepository tokenRepository;
  private final EmailService emailService;

  private static final long EXPIRE_MINUTES = 30L; // 토큰 만료 시간: 30분

  /**
   * 이메일 인증 토큰을 생성하고(DB 저장) 인증 메일을 발송합니다.
   *
   * @param userEmail 가입 대상 이메일
   * @param userName 가입자 이름
   * @param verificationUrl 인증 완료 후 리다이렉트 또는 인증 처리를 위한 프론트/백엔드 기본 주소
   */
  @Transactional
  public void generateTokenAndSendEmail(String userEmail, String userName, String verificationUrl) {
    // 기존 유효한 토큰은 삭제 또는 무효 처리
    // 동일 이메일 토큰은 삭제 후 재발급
    tokenRepository.findByUserEmail(userEmail).ifPresent(tokenRepository::delete);

    String tokenValue = UUID.randomUUID().toString();
    LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES);

    EmailVerificationTokenEntity tokenEntity =
        EmailVerificationTokenEntity.builder()
            .userEmail(userEmail)
            .token(tokenValue)
            .expiresAt(expiresAt)
            .build();

    tokenRepository.save(tokenEntity);

    // 이메일 발송용 DTO 생성
    WorkspaceVerificationEmailMessage message =
        WorkspaceVerificationEmailMessage.builder()
            .userEmail(userEmail)
            .userName(userName)
            .verificationToken(tokenValue)
            .verificationUrl(verificationUrl)
            .expiresAt(expiresAt)
            .build();

    // 이메일 발송 (이메일 서비스는 비동기로 처리됨)
    try {
      emailService.sendWorkspaceVerificationEmail("no-reply@calfit.com", message);
    } catch (Exception e) {
      log.error("인증 이메일 발송 중 오류 발생: {}", e.getMessage());
      throw new RuntimeException("이메일 발송에 실패하여 인증 링크를 생성하지 못했습니다.", e);
    }
  }

  /**
   * 이메일에서 전송된 토큰을 검증하고, 성공 시 verified 상태로 변경
   *
   * @param token 사용자가 이메일에서 클릭하여 전달된 토큰 값
   * @return 검증 성공 여부
   */
  @Transactional
  public boolean verifyEmailToken(String token) {
    Optional<EmailVerificationTokenEntity> optionalToken = tokenRepository.findByToken(token);

    if (optionalToken.isEmpty()) {
      log.warn("인증 토큰을 찾을 수 없습니다. token: {}", token);
      return false;
    }

    EmailVerificationTokenEntity tokenEntity = optionalToken.get();

    // 이미 인증되었는지 확인
    if (tokenEntity.isVerified()) {
      log.info("이미 인증된 토큰입니다. userEmail: {}", tokenEntity.getUserEmail());
      return true;
    }

    // 만료 여부 확인
    if (tokenEntity.isExpired(LocalDateTime.now())) {
      log.warn("인증 토큰이 만료되었습니다. userEmail: {}", tokenEntity.getUserEmail());
      return false;
    }

    // 검증 성공 -> 상태 업데이트
    tokenEntity.verify();
    log.info("이메일 인증 성공! userEmail: {}", tokenEntity.getUserEmail());

    return true;
  }
}
