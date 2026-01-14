package com.team.voteland.storage.db.core.vote;

import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.storage.db.core.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "votes", indexes = { @Index(name = "idx_vote", columnList = "user_id") })
public class VoteEntity extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private VoteType voteType;

    @Column(name = "deadline", nullable = false)
    private LocalDateTime deadline;

    protected VoteEntity() {
    }

    public VoteEntity(Long userId, String title, String description, VoteType voteType, LocalDateTime deadline) {
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.voteType = voteType;
        this.deadline = deadline;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public VoteType getVoteType() {
        return voteType;
    }

    public LocalDateTime getDeadline() {
        return deadline;
    }

    public VoteStatus getVoteStatus() {
        return LocalDateTime.now().isBefore(deadline) ? VoteStatus.OPEN : VoteStatus.CLOSED;
    }

}
