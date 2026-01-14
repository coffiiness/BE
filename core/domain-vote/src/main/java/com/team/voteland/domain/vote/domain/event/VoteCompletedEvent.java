package com.team.voteland.domain.vote.domain.event;

import com.team.voteland.support.event.DomainEvent;

/**
 * 투표 완료 이벤트 (틀) 실제 투표 기능 구현 시 사용
 */
public record VoteCompletedEvent(Long voteId, Long userId, String voteTitle, long occurredAt) implements DomainEvent {

    public static VoteCompletedEvent of(Long voteId, Long userId, String voteTitle) {
        return new VoteCompletedEvent(voteId, userId, voteTitle, System.currentTimeMillis());
    }

}
