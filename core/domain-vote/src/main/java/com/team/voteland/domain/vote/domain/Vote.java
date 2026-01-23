package com.team.voteland.domain.vote.domain;

import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.domain.vote.api.v1.response.VoteInfoResponse;
import com.team.voteland.storage.db.core.vote.VoteEntity;

import java.time.LocalDateTime;

public record Vote(Long id, String title, String description, VoteType voteType, VoteStatus voteStatus,
        LocalDateTime createdAt, LocalDateTime deadline) {

    public static Vote from(VoteEntity entity) {
        return new Vote(entity.getId(), entity.getTitle(), entity.getDescription(), entity.getVoteType(),
                entity.getVoteStatus(), entity.getCreatedAt(), entity.getDeadline());
    }
}
