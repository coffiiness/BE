package com.coffiness.calfit.domain;

import com.coffiness.calfit.model.GoogleTokenModel;
import com.coffiness.calfit.port.GoogleOAuthPort;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarEntity;
import com.coffiness.calfit.storage.db.core.calendar.ExternalCalendarRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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

    public void connectGoogleCalendar(String authCode, Long userId) {

        // 구글 토큰 발급 및 이메일 추출
        GoogleTokenModel tokenModel = googleOAuthPort.exchangeToken(authCode);
        String googleEmail = tokenModel.email();

        // 토큰 만료 시간 계산
        // 만료 시간 버퍼 확보를 위한 2분 차감
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(tokenModel.expiresIn() - 120);

        // 기존 캘린더 연동 내역 조회
        Optional<ExternalCalendarEntity> existingCalendar =
                externalCalendarRepository.findByUserIdAndCalendarId(userId, googleEmail);

        // 연동 정보 저장
        if(existingCalendar.isPresent()) {
            // 기존 연동 유지
            ExternalCalendarEntity existingEmail = existingCalendar.get();
            existingEmail.updateAuthTokens(tokenModel.accessToken(), tokenModel.refreshToken(), expiresAt);

            externalCalendarRepository.save(existingEmail);
        } else {
            // 최초 연동
            ExternalCalendarEntity newCalendar = ExternalCalendarEntity.builder()
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
