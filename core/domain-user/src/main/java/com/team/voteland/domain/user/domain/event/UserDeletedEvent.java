package com.team.voteland.domain.user.domain.event;

import com.team.voteland.support.event.DomainEvent;

/**
 * 회원 탈퇴 이벤트
 */
public record UserDeletedEvent(Long userId, String email, long occurredAt) implements DomainEvent {

    public static UserDeletedEvent of(Long userId, String email) {
        return new UserDeletedEvent(userId, email, System.currentTimeMillis());
    }

}
