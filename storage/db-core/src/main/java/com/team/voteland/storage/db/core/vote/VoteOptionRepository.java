package com.team.voteland.storage.db.core.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteOptionRepository extends JpaRepository<VoteOptionEntity, Long> {

    long countByVoteId(Long voteId);

    List<VoteOptionEntity> findAllByVoteId(Long voteId);

}
