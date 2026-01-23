package com.team.voteland.storage.db.core.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteRecordRepository extends JpaRepository<VoteRecordEntity, Long> {

    long countByVoteId(Long voteId);

    java.util.List<VoteRecordEntity> findAllByVoteIdAndUserId(Long voteId, Long userId);

    Optional<VoteRecordEntity> findTopByVoteIdOrderByCreatedAtDesc(Long voteId);

    long countByVoteOptionId(Long voteOptionId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT r.userId) FROM VoteRecordEntity r WHERE r.voteId = :voteId")
    long countDistinctUserIdByVoteId(@org.springframework.data.repository.query.Param("voteId") Long voteId);

    void deleteByVoteIdAndUserId(Long voteId, Long userId);

    boolean existsByVoteIdAndUserId(Long voteId, Long userId);

}
