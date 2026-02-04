package com.coffiness.calfit.domain.vote.api.v1.response;

import com.coffiness.calfit.core.enums.VoteStatus;
import com.coffiness.calfit.core.enums.VoteType;

import java.time.LocalDateTime;
import java.util.List;

public record VoteDetailResponse(Long id, VoteStatus voteStatus, String title, String description,
        LocalDateTime createdAt, LocalDateTime deadline, String remainingTime, VoteType voteType,
        Integer participantCount, List<VoteOptionResponse> options) {
}
