package com.team.voteland.domain.vote.domain.event;

import com.team.voteland.domain.user.domain.event.UserDeletedEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 회원 탈퇴 이벤트 핸들러 User 도메인에서 발행한 UserDeletedEvent를 비동기로 처리
 */
@Component
public class UserDeletedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(UserDeletedEventHandler.class);

    @Async
    @EventListener
    public void handle(UserDeletedEvent event) {
        log.info("회원 탈퇴 이벤트 수신 - userId: {}, email: {}", event.userId(), event.email());

        // TODO: 해당 회원의 투표 기록 삭제 로직 구현
        // 예: voteRepository.deleteAllByUserId(event.userId());

        log.info("회원 {} 의 투표 기록 삭제 처리 완료", event.userId());
    }

}
