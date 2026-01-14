package com.team.voteland.storage.db.core.vote;

import com.team.voteland.storage.db.core.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "vote_options",
        indexes = { @Index(name = "idx_vote_options", columnList = "vote_id, sequence", unique = true) })
public class VoteOptionEntity extends BaseEntity {

    @Column(name = "vote_id", nullable = false)
    private Long voteId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "vote_count", nullable = false)
    private Integer voteCount = 0;

    protected VoteOptionEntity() {
    }

    public VoteOptionEntity(Long voteId, String content, Integer sequence) {
        this.voteId = voteId;
        this.content = content;
        this.sequence = sequence;
    }

    public Long getVoteId() {
        return voteId;
    }

    public String getContent() {
        return content;
    }

    public Integer getSequence() {
        return sequence;
    }

    public Integer getVoteCount() {
        return voteCount;
    }

}
