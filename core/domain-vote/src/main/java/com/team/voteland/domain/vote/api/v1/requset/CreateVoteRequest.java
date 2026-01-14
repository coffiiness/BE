package com.team.voteland.domain.vote.api.v1.requset;

import com.team.voteland.core.enums.VoteType;

import java.time.LocalDateTime;
import java.util.List;

public record CreateVoteRequest(String title, String description, VoteType voteType, List<String> options,
        LocalDateTime deadline) {

}
