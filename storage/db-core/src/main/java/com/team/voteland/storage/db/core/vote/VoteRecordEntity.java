package com.team.voteland.storage.db.core.vote;

import com.team.voteland.storage.db.core.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "vote_records",
        indexes = { @Index(name = "idx_vote_records", columnList = "vote_id, user_id, option_id", unique = true) })
public class VoteRecordEntity extends BaseEntity {

    @Column(name = "vote_id")
    private Long voteId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "option_id")
    private Long voteOptionId;

}
