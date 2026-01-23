package com.team.voteland.domain.vote.api.v1.response;

public record VoteOptionResultResponse(Long id, String content, Integer voteCount, double voteRatio, Integer rank) {
}
