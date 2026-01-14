package com.team.voteland.domain.vote.domain;

import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.storage.db.core.vote.VoteEntity;

import java.time.LocalDateTime;

public record VoteInfo(Long id, String title, String description, VoteType voteType, Integer optionCount,
        Integer voterCount, VoteStatus voteStatus, LocalDateTime createdAt) {

    public static VoteInfo of(Vote vote, Integer optionCount, Integer voterCount) {
        return new VoteInfo(vote.id(), vote.title(), vote.description(), vote.voteType(), optionCount, voterCount,
                vote.voteStatus(), vote.createdAt());
    }
}
