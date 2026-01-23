package com.team.voteland.api.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.domain.vote.api.v1.request.CreateVoteRequest;
import com.team.voteland.domain.vote.api.v1.request.VoteSubmitRequest;
import com.team.voteland.domain.vote.api.v1.response.VoteDetailResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteInfoResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteSubmitResponse;
import org.springframework.core.env.Environment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public record VoteFixture(BaseFixture base) {

    public static VoteFixture create(Environment environment, ObjectMapper objectMapper) {
        return new VoteFixture(BaseFixture.create(environment, objectMapper));
    }

    // ==================== Random Data Generators ====================

    public String randomTitle() {
        return "title-" + UUID.randomUUID();
    }

    public String randomDescription() {
        return "description-" + UUID.randomUUID();
    }

    public VoteType randomVoteType() {
        int random = new Random().nextInt(VoteType.values().length);
        return VoteType.values()[random];
    }

    public List<String> randomOptions() {
        List<String> options = new ArrayList<>();
        int random = new Random().nextInt(5) + 2;
        for (int i = 0; i < random; i++) {
            options.add("option-" + i + "-" + UUID.randomUUID());
        }
        return options;
    }

    public LocalDateTime randomDeadline() {
        return LocalDateTime.now().plusHours(new Random().nextInt(6) + 1);
    }

    // ==================== API Calls ====================

    public ApiResponse<Void> createVote(String title, String description, VoteType voteType, List<String> options,
            LocalDateTime deadline) {
        CreateVoteRequest request = new CreateVoteRequest(title, description, voteType, options, deadline);
        return base.post("/api/v1/votes", request, Void.class);
    }

    public ApiResponse<Void> createVote(String title, String description, VoteType voteType, List<String> options,
            LocalDateTime deadline, String token) {
        CreateVoteRequest request = new CreateVoteRequest(title, description, voteType, options, deadline);
        return base.post("/api/v1/votes", request, token, Void.class);
    }

    public Long createVoteAndGetId(String token) {
        CreateVoteRequest request = new CreateVoteRequest(randomTitle(), randomDescription(), randomVoteType(),
                randomOptions(), randomDeadline());
        base().post("/api/v1/votes", request, token, Void.class);

        ApiResponse<VoteInfoResponse[]> listResponse = base().get("/api/v1/votes", token, VoteInfoResponse[].class);
        return listResponse.getData()[0].id();
    }

    public ApiResponse<VoteInfoResponse[]> getVoteInfos() {
        return base().get("/api/v1/votes", VoteInfoResponse[].class);
    }

    public ApiResponse<VoteInfoResponse[]> getVoteInfos(String token) {
        return base().get("/api/v1/votes", token, VoteInfoResponse[].class);
    }

    public ApiResponse<VoteDetailResponse> getVoteDetail(Long voteId) {
        return base().get("/api/v1/votes/" + voteId, VoteDetailResponse.class);
    }

    public ApiResponse<VoteDetailResponse> getVoteDetail(Long voteId, String token) {
        return base().get("/api/v1/votes/" + voteId, token, VoteDetailResponse.class);
    }

    public ApiResponse<VoteSubmitResponse> submitVote(Long voteId, List<Long> itemIds, String token) {
        VoteSubmitRequest request = new VoteSubmitRequest(itemIds);
        return base.post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);
    }
}
