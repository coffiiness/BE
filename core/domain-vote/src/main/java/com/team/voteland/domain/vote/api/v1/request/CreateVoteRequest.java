package com.team.voteland.domain.vote.api.v1.request;

import com.team.voteland.core.enums.VoteType;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public record CreateVoteRequest(@NotBlank(message = "제목은 필수입니다") String title,

        String description,

        @NotNull(message = "투표 유형은 필수입니다") VoteType voteType,

        @NotNull(message = "투표 옵션은 필수입니다") @Size(min = 2, message = "투표 옵션은 최소 2개 이상이어야 합니다") List<String> options,

        @NotNull(message = "마감 시간은 필수입니다") @Future(message = "마감 시간은 미래 시간이어야 합니다") LocalDateTime deadline) {

}
