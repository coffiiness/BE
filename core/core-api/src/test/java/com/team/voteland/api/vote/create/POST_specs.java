package com.team.voteland.api.vote.create;

import com.team.voteland.api.VotelandApiTest;
import com.team.voteland.api.fixture.UserFixture;
import com.team.voteland.api.fixture.VoteFixture;
import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.core.support.response.ResultType;
import com.team.voteland.domain.vote.api.v1.request.CreateVoteRequest;
import com.team.voteland.storage.db.core.vote.VoteEntity;
import com.team.voteland.storage.db.core.vote.VoteOptionRepository;
import com.team.voteland.storage.db.core.vote.VoteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@VotelandApiTest
@DisplayName("POST /api/v1/votes")
public class POST_specs {

    @Test
    void 올바르게_요청하면_SUCCESS를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 인증_토큰_없이_요청하면_ERROR를_반환한다(@Autowired VoteFixture voteFixture) {
        // Arrange
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 잘못된_토큰으로_요청하면_ERROR를_반환한다(@Autowired VoteFixture voteFixture) {
        // Arrange
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, "invalid-token", Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void title_속성이_지정되지_않으면_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(null, voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "   " })
    void title_속성이_빈_문자열이거나_공백만_있으면_ERROR를_반환한다(String title, @Autowired UserFixture userFixture,
            @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(title, voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void voteType_속성이_지정되지_않으면_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                null, voteFixture.randomOptions(), voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void options_속성이_지정되지_않으면_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), null, voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void options_속성이_비어있으면_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), Collections.emptyList(), voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void options_속성이_1개만_있으면_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), List.of("option1"), voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @ParameterizedTest
    @ValueSource(ints = { 2, 3, 5, 10 })
    void options_속성이_2개_이상이면_SUCCESS를_반환한다(int optionCount, @Autowired UserFixture userFixture,
            @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        List<String> options = java.util.stream.IntStream.range(0, optionCount).mapToObj(i -> "option-" + i).toList();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), options, voteFixture.randomDeadline());

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void deadline_속성이_지정되지_않으면_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), null);

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void deadline_속성이_과거_시간이면_ERROR를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), LocalDateTime.now().minusHours(1));

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void deadline_속성이_미래_시간이면_SUCCESS를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        CreateVoteRequest request = new CreateVoteRequest(voteFixture.randomTitle(), voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), LocalDateTime.now().plusDays(7));

        // Act
        ApiResponse<?> response = voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 투표_생성_후_조회하면_데이터가_존재한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        String title = voteFixture.randomTitle();
        CreateVoteRequest request = new CreateVoteRequest(title, voteFixture.randomDescription(),
                voteFixture.randomVoteType(), voteFixture.randomOptions(), voteFixture.randomDeadline());

        // Act
        voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElse(null);
        assertThat(vote).isNotNull();
        assertThat(vote.getTitle()).isEqualTo(title);
    }

    @Test
    void 투표_생성_후_옵션_개수가_일치한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture,
            @Autowired VoteRepository voteRepository, @Autowired VoteOptionRepository voteOptionRepository) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        String title = voteFixture.randomTitle();
        List<String> options = List.of("option1", "option2", "option3");
        CreateVoteRequest request = new CreateVoteRequest(title, voteFixture.randomDescription(),
                voteFixture.randomVoteType(), options, voteFixture.randomDeadline());

        // Act
        voteFixture.base().post("/api/v1/votes", request, token, Void.class);

        // Assert
        VoteEntity vote = voteRepository.findAll()
            .stream()
            .filter(v -> v.getTitle().equals(title))
            .findFirst()
            .orElseThrow();
        long optionCount = voteOptionRepository.countByVoteId(vote.getId());
        assertThat(optionCount).isEqualTo(options.size());
    }

}
