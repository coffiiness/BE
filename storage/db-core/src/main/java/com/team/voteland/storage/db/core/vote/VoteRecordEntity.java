package com.team.voteland.storage.db.core.vote;

import com.team.voteland.storage.db.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "vote_records", indexes = {
        @Index(name = "idx_unique_vote_record", columnList = "vote_id, user_id, vote_option_id", unique = true) })
public class VoteRecordEntity extends BaseEntity {

    @Column(name = "vote_id")
    private Long voteId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "vote_option_id")
    private Long voteOptionId;

    protected VoteRecordEntity() {
    }

    public VoteRecordEntity(Long voteId, Long userId, Long voteOptionId) {
        this.voteId = voteId;
        this.userId = userId;
        this.voteOptionId = voteOptionId;
    }

    public Long getVoteId() {
        return voteId;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getVoteOptionId() {
        return voteOptionId;
    }

}
