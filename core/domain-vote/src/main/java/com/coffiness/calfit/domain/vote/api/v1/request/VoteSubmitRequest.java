package com.coffiness.calfit.domain.vote.api.v1.request;

import java.util.List;

public record VoteSubmitRequest(List<Long> itemIds) {
}
