package com.coffiness.calfit.domain.vote.api.v1.response;

import com.coffiness.calfit.core.enums.VoteStatus;
import com.coffiness.calfit.core.enums.VoteType;
import com.coffiness.calfit.domain.vote.domain.Vote;
import com.coffiness.calfit.domain.vote.domain.VoteInfo;
import com.coffiness.calfit.storage.db.core.vote.VoteEntity;

import java.time.LocalDateTime;

public record VoteInfoResponse(Long id, String title, String description, VoteType voteType, Integer optionCount,
        Integer voterCount, VoteStatus voteStatus, LocalDateTime createdAt) {

    public static VoteInfoResponse from(VoteInfo voteInfo) {
        return new VoteInfoResponse(voteInfo.id(), voteInfo.title(), voteInfo.description(), voteInfo.voteType(),
                voteInfo.optionCount(), voteInfo.voterCount(), voteInfo.voteStatus(), voteInfo.createdAt());
    }
}