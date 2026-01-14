package com.team.voteland.storage.db.core.vote;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteRecordRepository extends JpaRepository<VoteRecordEntity, Long> {

    long countByVoteId(Long voteId);

}
