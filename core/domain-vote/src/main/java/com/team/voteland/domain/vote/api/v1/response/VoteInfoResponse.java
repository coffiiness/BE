package com.team.voteland.domain.vote.api.v1.response;

import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.domain.vote.domain.Vote;
import com.team.voteland.domain.vote.domain.VoteInfo;
import com.team.voteland.storage.db.core.vote.VoteEntity;

import java.time.LocalDateTime;

public record VoteInfoResponse(Long id, String title, String description, VoteType voteType, Integer optionCount,
        Integer voterCount, VoteStatus voteStatus, LocalDateTime createdAt) {

    public static VoteInfoResponse from(VoteInfo voteInfo) {
        return new VoteInfoResponse(voteInfo.id(), voteInfo.title(), voteInfo.description(), voteInfo.voteType(),
                voteInfo.optionCount(), voteInfo.voterCount(), voteInfo.voteStatus(), voteInfo.createdAt());
    }
}