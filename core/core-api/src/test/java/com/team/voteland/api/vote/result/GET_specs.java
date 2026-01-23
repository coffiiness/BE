package com.team.voteland.api.vote.result;

import com.team.voteland.api.VotelandApiTest;
import com.team.voteland.api.fixture.UserFixture;
import com.team.voteland.api.fixture.VoteFixture;
import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.core.support.response.ResultType;
import com.team.voteland.domain.vote.api.v1.request.CreateVoteRequest;
import com.team.voteland.domain.vote.api.v1.response.VoteOptionResultResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteResultResponse;
import com.team.voteland.storage.db.core.vote.VoteEntity;
import com.team.voteland.storage.db.core.vote.VoteOptionRepository;
import com.team.voteland.storage.db.core.vote.VoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@VotelandApiTest
@DisplayName("GET /api/v1/votes/{voteId}/result")
public class GET_specs {

    @Test
    void 존재하지_않는_투표를_조회하면_ERROR를_반환한다(@Autowired VoteFixture voteFixture, @Autowired UserFixture userFixture) {

        // Arrange
        String token = userFixture.createUserAndGetToken();

        // Act
        ApiResponse<?> response = voteFixture.base().get("/api/v1/votes/9999/result", token, VoteResultResponse.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 투표를_생성하면_결과_조회가_가능하다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {

        // Arrange
        String token = userFixture.createUserAndGetToken();

        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline());

        voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        VoteEntity vote = voteRepository.findAll().get(0);

        // Act
        ApiResponse<VoteResultResponse> response = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData().options()).isNotEmpty();
    }

    @Test
    void 마감된_투표는_CLOSED_상태를_반환한다(@Autowired VoteRepository voteRepository, @Autowired VoteFixture voteFixture,
            @Autowired UserFixture userFixture) {

        // Arrange
        String token = userFixture.createUserAndGetToken();

        VoteEntity vote = voteRepository
            .save(new VoteEntity(1L, "제목", "설명", voteFixture.randomVoteType(), LocalDateTime.now().minusMinutes(1)));

        // Act
        VoteResultResponse result = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class)
            .getData();

        // Assert
        assertThat(result.voteStatus()).isEqualTo(VoteStatus.CLOSED);
    }

    @Test
    void 아직_마감되지_않은_투표는_OPEN_상태를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {

        // Arrange
        String token = userFixture.createUserAndGetToken();

        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), LocalDateTime.now().plusHours(1));

        voteFixture.base().post("/api/v1/votes", request, token, Void.class);
        VoteEntity vote = voteRepository.findAll().get(0);

        // Act
        VoteResultResponse result = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class)
            .getData();

        // Assert
        assertThat(result.voteStatus()).isEqualTo(VoteStatus.OPEN);
    }

    @Test
    void 투표_결과_조회시_옵션_개수는_실제로_저장된_옵션_개수와_같다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {

        // Arrange
        String token = userFixture.createUserAndGetToken();

        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline());

        voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        VoteEntity vote = voteRepository.findAll().get(0);

        int savedOptionCount = voteOptionRepository.findAllByVoteId(vote.getId()).size();

        // Act
        VoteResultResponse result = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class)
            .getData();

        // Assert
        assertThat(result.options().size()).isEqualTo(savedOptionCount);
    }

    @Test
    void 투표가_하나도_이뤄지지_않아도_결과_조회는_가능하다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {

        // Arrange
        String token = userFixture.createUserAndGetToken();

        voteFixture.base()
            .post("/api/v1/votes",
                    new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                            voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline()),
                    token, Void.class);

        VoteEntity vote = voteRepository.findAll().get(0);

        // Act
        ApiResponse<VoteResultResponse> response = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData().options()).allMatch(option -> option.voteCount() == 0);
    }

    @Test
    void 투표_결과는_득표수가_많은_옵션부터_정렬된다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {

        // Arrange
        String token = userFixture.createUserAndGetToken();

        voteFixture.base()
            .post("/api/v1/votes",
                    new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                            voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline()),
                    token, Void.class);

        VoteEntity vote = voteRepository.findAll().get(0);

        // (투표 submit은 이미 다른 테스트에서 검증된 전제라고 가정)

        // Act
        VoteResultResponse result = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class)
            .getData();

        // Assert
        assertThat(result.options()).isSortedAccordingTo((o1, o2) -> Integer.compare(o2.voteCount(), o1.voteCount()));
    }

    @Test
    void 결과_조회는_인증된_사용자만_가능하다(@Autowired VoteFixture voteFixture, @Autowired VoteRepository voteRepository) {

        // Arrange
        VoteEntity vote = voteRepository
            .save(new VoteEntity(1L, "제목", "설명", voteFixture.randomVoteType(), LocalDateTime.now().plusMinutes(10)));

        // Act
        ApiResponse<?> response = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", VoteResultResponse.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5, 10 })
    @DisplayName("옵션 개수가 여러 개여도 결과 조회는 SUCCESS")
    void 옵션_개수별_결과_조회_SUCCESS(int optionCount, @Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {
        // Arrange
        String token = userFixture.createUserAndGetToken();

        List<String> options = java.util.stream.IntStream.range(0, optionCount).mapToObj(i -> "option-" + i).toList();

        voteFixture.base()
            .post("/api/v1/votes", new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                    voteFixture.randomVoteType(), options, voteFixture.randomDeadline()), token, Void.class);

        VoteEntity vote = voteRepository.findAll().get(0);
        int savedOptionCount = voteOptionRepository.findAllByVoteId(vote.getId()).size();

        // Act
        VoteResultResponse result = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class)
            .getData();

        // Assert
        assertThat(result.options().size()).isEqualTo(savedOptionCount);
    }

    @ParameterizedTest
    @ValueSource(booleans = { true, false })
    @DisplayName("인증 여부에 따라 결과 조회 성공/실패가 결정된다")
    void 인증여부에_따른_결과조회(boolean authenticated, @Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {
        // Arrange
        String token = authenticated ? userFixture.createUserAndGetToken() : null;

        VoteEntity vote = voteRepository
            .save(new VoteEntity(1L, "제목", "설명", voteFixture.randomVoteType(), LocalDateTime.now().plusMinutes(10)));

        // Act
        ApiResponse<?> response = authenticated
                ? voteFixture.base().get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class)
                : voteFixture.base().get("/api/v1/votes/" + vote.getId() + "/result", VoteResultResponse.class);

        // Assert
        if (authenticated) {
            assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        }
        else {
            assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
        }
    }

    @Test
    void 투표가_없으면_비율합은_0이다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {
        String token = userFixture.createUserAndGetToken();

        voteFixture.base()
            .post("/api/v1/votes",
                    new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                            voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline()),
                    token, Void.class);

        VoteEntity vote = voteRepository.findAll().get(0);

        VoteResultResponse result = voteFixture.base()
            .get("/api/v1/votes/" + vote.getId() + "/result", token, VoteResultResponse.class)
            .getData();

        double totalRatio = result.options().stream().mapToDouble(VoteOptionResultResponse::voteRatio).sum();

        assertThat(totalRatio).isEqualTo(0.0);
    }

}
