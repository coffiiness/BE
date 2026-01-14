package com.team.voteland.storage.db.core.vote;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteOptionRepository extends JpaRepository<VoteOptionEntity, Integer> {

    long countByVoteId(Long voteId);

}
