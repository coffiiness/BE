package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.vote.api.v1.request.CreateVoteRequest;
import com.coffiness.calfit.domain.vote.api.v1.request.VoteSubmitRequest;
import com.coffiness.calfit.domain.vote.api.v1.response.VoteDetailResponse;
import com.coffiness.calfit.domain.vote.api.v1.response.VoteInfoResponse;
import com.coffiness.calfit.domain.vote.api.v1.response.VoteResultResponse;
import com.coffiness.calfit.domain.vote.api.v1.response.VoteStatusResponse;
import com.coffiness.calfit.domain.vote.api.v1.response.VoteSubmitResponse;
import com.coffiness.calfit.domain.vote.domain.VoteInfo;
import com.coffiness.calfit.domain.vote.domain.VoteService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class VoteController {

    private final VoteService voteService;

    @Autowired
    public VoteController(VoteService voteService) {
        this.voteService = voteService;
    }

    @PostMapping("/api/v1/votes")
    public ApiResponse<?> createVote(@AuthenticationPrincipal SecurityUser securityUser,
            @RequestBody CreateVoteRequest request) {
        voteService.createVote(securityUser.userId(), request.title(), request.description(), request.voteType(),
                request.options(), request.deadline());
        return ApiResponse.success();
    }

    @GetMapping("/api/v1/votes")
    public ApiResponse<List<VoteInfoResponse>> getVoteInfos() {
        List<VoteInfo> votes = voteService.getVoteInfos();
        List<VoteInfoResponse> response = votes.stream().map(VoteInfoResponse::from).toList();
        return ApiResponse.success(response);
    }

    @GetMapping("/api/v1/votes/{voteId}")
    public ApiResponse<VoteDetailResponse> getVoteDetail(@PathVariable Long voteId) {
        VoteDetailResponse response = voteService.getVoteDetail(voteId);
        return ApiResponse.success(response);
    }

    @GetMapping("/api/v1/votes/{voteId}/participation-status")
    public ApiResponse<VoteStatusResponse> getVoteStatus(@PathVariable Long voteId,
            @AuthenticationPrincipal SecurityUser securityUser) {
        VoteStatusResponse response = voteService.getVoteStatus(voteId, securityUser.userId());
        return ApiResponse.success(response);
    }

    @PostMapping("/api/v1/votes/{voteId}/submit")
    public ApiResponse<VoteSubmitResponse> submitVote(@PathVariable Long voteId,
            @AuthenticationPrincipal SecurityUser securityUser, @RequestBody VoteSubmitRequest request) {
        VoteSubmitResponse response = voteService.submitVote(voteId, securityUser.userId(), request);
        return ApiResponse.success(response);
    }

    @GetMapping("/api/v1/votes/{voteId}/result")
    public ApiResponse<VoteResultResponse> getVoteResult(@PathVariable Long voteId) {
        VoteResultResponse response = voteService.getVoteResult(voteId);
        return ApiResponse.success(response);
    }

}
