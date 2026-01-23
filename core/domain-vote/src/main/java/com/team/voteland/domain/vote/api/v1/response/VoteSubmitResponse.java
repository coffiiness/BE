package com.team.voteland.domain.vote.api.v1.response;

import java.util.List;

public record VoteSubmitResponse(String message, List<Long> votedItems) {
}
