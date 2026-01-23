package com.team.voteland.api.vote.submit;

import com.team.voteland.api.VotelandApiTest;
import com.team.voteland.api.fixture.UserFixture;
import com.team.voteland.api.fixture.VoteFixture;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.core.support.response.ResultType;
import com.team.voteland.domain.vote.api.v1.request.VoteSubmitRequest;
import com.team.voteland.domain.vote.api.v1.response.VoteSubmitResponse;
import com.team.voteland.storage.db.core.vote.VoteEntity;
import com.team.voteland.storage.db.core.vote.VoteOptionEntity;
import com.team.voteland.storage.db.core.vote.VoteOptionRepository;
import com.team.voteland.storage.db.core.vote.VoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@VotelandApiTest
@DisplayName("POST api/v1/votes/{voteId}/submit")
public class POST_specs {

    @Test
    void 단일투표_성공시_SUCCESS를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        VoteOptionEntity option = voteOptionRepository.findAllByVoteId(voteId).get(0);
        Long optionId = option.getId();

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(optionId));
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 복수투표_성공시_SUCCESS를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.MULTIPLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        VoteOptionEntity option1 = voteOptionRepository.findAllByVoteId(voteId).get(0);
        VoteOptionEntity option2 = voteOptionRepository.findAllByVoteId(voteId).get(1);

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(option1.getId(), option2.getId()));
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 기존_투표_변경시_SUCCESS를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        List<VoteOptionEntity> options = voteOptionRepository.findAllByVoteId(voteId);
        Long optionId1 = options.get(0).getId();
        Long optionId2 = options.get(1).getId();

        VoteSubmitRequest request1 = new VoteSubmitRequest(List.of(optionId1));
        voteFixture.base().post("/api/v1/votes/" + voteId + "/submit", request1, token, VoteSubmitResponse.class);

        VoteSubmitRequest request2 = new VoteSubmitRequest(List.of(optionId2));
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request2, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 같은_옵션_재투표시_SUCCESS를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        List<VoteOptionEntity> options = voteOptionRepository.findAllByVoteId(voteId);
        Long optionId1 = options.get(0).getId();

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(optionId1));
        voteFixture.base().post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);

        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 존재하지_않는_투표_ID로_요청시_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();

        List<VoteOptionEntity> options = voteOptionRepository.findAllByVoteId(vote.getId());
        Long optionId = options.get(0).getId();

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(optionId));
        Long nonExistentVoteId = Long.MAX_VALUE;
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + nonExistentVoteId + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 마감된_투표에_참여시_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        ReflectionTestUtils.setField(vote, "deadline", LocalDateTime.now().minusDays(1));
        voteRepository.saveAndFlush(vote);

        List<VoteOptionEntity> options = voteOptionRepository.findAllByVoteId(voteId);
        Long optionId = options.get(0).getId();

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(optionId));
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 항목_미선택시_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        VoteSubmitRequest request = new VoteSubmitRequest(Collections.emptyList());
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 단일_투표선택지에_중복투표시_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        VoteOptionEntity option1 = voteOptionRepository.findAllByVoteId(voteId).get(0);
        VoteOptionEntity option2 = voteOptionRepository.findAllByVoteId(voteId).get(1);

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(option1.getId(), option2.getId()));
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 존재하지않는_옵션ID를_선택시_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(Long.MAX_VALUE));
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 다른_투표의_옵션ID를_선택시_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title1 = voteFixture.randomTitle();
        voteFixture.createVote(title1, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        String title2 = voteFixture.randomTitle();
        voteFixture.createVote(title2, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote1 = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title1))
            .findFirst()
            .orElseThrow();
        VoteEntity vote2 = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title2))
            .findFirst()
            .orElseThrow();

        Long voteId2 = vote2.getId();

        VoteOptionEntity optionFromVote1 = voteOptionRepository.findAllByVoteId(vote1.getId()).get(0);
        Long optionIdFromVote1 = optionFromVote1.getId();

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(optionIdFromVote1));
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId2 + "/submit", request, token, VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 잘못된_토큰으로_요청하면_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        String token = userFixture.createUserAndGetToken();

        String title = voteFixture.randomTitle();
        voteFixture.createVote(title, voteFixture.randomDescription(), VoteType.SINGLE, voteFixture.randomOptions(),
                voteFixture.randomDeadline(), token);

        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        Long voteId = vote.getId();

        VoteOptionEntity option = voteOptionRepository.findAllByVoteId(voteId).get(0);

        VoteSubmitRequest request = new VoteSubmitRequest(List.of(option.getId()));
        ApiResponse<VoteSubmitResponse> response = voteFixture.base()
            .post("/api/v1/votes/" + voteId + "/submit", request, "invalid-token", VoteSubmitResponse.class);

        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

}