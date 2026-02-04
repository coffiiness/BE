package com.coffiness.calfit.domain.vote.api.v1.response;

import com.coffiness.calfit.core.enums.VoteStatus;

import java.time.LocalDateTime;
import java.util.List;

public record VoteResultResponse(Long id, String title, String description, VoteStatus voteStatus,
        Integer participantCount, LocalDateTime deadline, LocalDateTime lastUpdatedAt,
        List<VoteOptionResultResponse> options) {
}
