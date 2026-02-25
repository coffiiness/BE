package com.coffiness.calfit.domain;

import com.coffiness.calfit.model.GoogleTokenModel;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarEntity;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/*
* 구글 캘린더 연동 서비스 클래스
* 프론트에서 넘어온 authCode를 구글 토큰으로 교환 후 DB에 저장
* */
@Service
@RequiredArgsConstructor
public class CalendarConnectService {

    private final GoogleOAuthPort googleOAuthPort;
    private final ExternalCalendarRepository externalCalendarRepository;

    @Transactional
    public void connectGoogleCalendar(String authCode, Long userId, String tenantId) {

        // 구글 토큰 발급 및 이메일 추출
        // TODO : 트랜잭션 분리
        GoogleTokenModel tokenModel = googleOAuthPort.exchangeToken(authCode);
        String googleEmail = tokenModel.email();

        // 토큰 만료 시간 계산
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenModel.expiresIn());

        // 기존 캘린더 연동 내역 조회
        Optional<ExternalCalendarEntity> optionalEmail =
                externalCalendarRepository.findByTenantIdAndUserIdAndCalendarId(tenantId, userId, googleEmail);

        // 연동 정보 저장
        if(optionalEmail.isPresent()) {
            // 기존 연동 유지
            ExternalCalendarEntity existingEmail = optionalEmail.get();
            existingEmail.updateAuthTokens(tokenModel.accessToken(), tokenModel.refreshToken(), expiresAt);
        } else {
            // 최초 연동
            ExternalCalendarEntity newCalendar = ExternalCalendarEntity.builder()
                    .tenantId(tenantId)
                    .userId(userId)
                    .calendarId(googleEmail)
                    .accessToken(tokenModel.accessToken())
                    .refreshToken(tokenModel.refreshToken())
                    .tokenExpiresAt(expiresAt)
                    .isSyncEnabled(true)
                    .build();

            externalCalendarRepository.save(newCalendar);
        }
    }
}
