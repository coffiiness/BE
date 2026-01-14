package com.team.voteland.core.api.controller.v1;

import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.domain.vote.api.v1.requset.CreateVoteRequest;
import com.team.voteland.domain.vote.api.v1.response.VoteInfoResponse;
import com.team.voteland.domain.vote.domain.VoteInfo;
import com.team.voteland.domain.vote.domain.VoteService;
import com.team.voteland.support.security.jwt.SecurityUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
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

}
