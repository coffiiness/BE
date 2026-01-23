package com.team.voteland.api.vote.info;

import com.team.voteland.api.VotelandApiTest;
import com.team.voteland.api.fixture.UserFixture;
import com.team.voteland.api.fixture.VoteFixture;
import com.team.voteland.core.enums.VoteStatus;
import com.team.voteland.core.enums.VoteType;
import com.team.voteland.core.support.response.ApiResponse;
import com.team.voteland.core.support.response.ResultType;
import com.team.voteland.domain.vote.api.v1.response.VoteDetailResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteInfoResponse;
import com.team.voteland.domain.vote.api.v1.response.VoteOptionResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@VotelandApiTest
@DisplayName("GET /api/v1/votes")
public class GET_specs {

    @Test
    @DisplayName("올바르게 요청하면 성공 응답을 반환한다")
    void 투표_목록_조회시_올바르게_요청하면_SUCCESS를_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();

        // Act
        ApiResponse<VoteInfoResponse[]> response = voteFixture.getVoteInfos(token);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    @DisplayName("투표 목록을 전체 조회한다")
    void 투표_목록을_전체_조회한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        voteFixture.createVote(voteFixture.randomTitle(), voteFixture.randomDescription(), voteFixture.randomVoteType(),
                voteFixture.randomOptions(), voteFixture.randomDeadline(), token);
        voteFixture.createVote(voteFixture.randomTitle(), voteFixture.randomDescription(), voteFixture.randomVoteType(),
                voteFixture.randomOptions(), voteFixture.randomDeadline(), token);

        // Act
        ApiResponse<VoteInfoResponse[]> response = voteFixture.getVoteInfos(token);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("참여자 수가 정확하게 반환된다")
    void 참여자_수가_정확하게_반환된다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String creatorToken = userFixture.createUserAndGetToken();
        String title = voteFixture.randomTitle();
        List<String> options = List.of("option1", "option2");
        voteFixture.createVote(title, voteFixture.randomDescription(), voteFixture.randomVoteType(), options,
                LocalDateTime.now().plusDays(1), creatorToken);

        // 첫 번째 조회 (참여자 0명)
        ApiResponse<VoteInfoResponse[]> initialResponse = voteFixture.getVoteInfos(creatorToken);
        VoteInfoResponse initialVote = findVoteByTitle(initialResponse.getData(), title);
        assertThat(initialVote).isNotNull();
        assertThat(initialVote.voterCount()).isEqualTo(0);

        // 투표 참여
        String voterToken = userFixture.createUserAndGetToken();
        Long voteId = initialVote.id();

        // 옵션 ID 조회를 위해 상세 조회
        ApiResponse<VoteDetailResponse> detailResponse = voteFixture.getVoteDetail(voteId, voterToken);
        List<VoteOptionResponse> voteOptions = detailResponse.getData().options();
        Long optionId = voteOptions.get(0).id();

        // 투표 제출
        voteFixture.submitVote(voteId, List.of(optionId), voterToken);

        // 두 번째 조회 (참여자 1명)
        ApiResponse<VoteInfoResponse[]> afterResponse = voteFixture.getVoteInfos(creatorToken);
        VoteInfoResponse afterVote = findVoteByTitle(afterResponse.getData(), title);
        assertThat(afterVote).isNotNull();
        assertThat(afterVote.voterCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("투표가 하나도 없을 때 빈 목록을 반환한다")
    void 투표가_하나도_없을_때_빈_목록을_반환한다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();

        // Act
        ApiResponse<VoteInfoResponse[]> response = voteFixture.getVoteInfos(token);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData()).isNotNull();
    }

    @Test
    @DisplayName("투표 정보가 정확하게 매핑되어 반환된다")
    void 투표_정보가_정확하게_매핑되어_반환된다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        String title = "Specific Title Check";
        String description = "Specific Description Check";
        VoteType type = VoteType.SINGLE;
        List<String> options = List.of("A", "B");
        LocalDateTime deadline = LocalDateTime.now().plusDays(2);

        voteFixture.createVote(title, description, type, options, deadline, token);

        // Act
        ApiResponse<VoteInfoResponse[]> response = voteFixture.getVoteInfos(token);
        VoteInfoResponse targetVote = findVoteByTitle(response.getData(), title);

        // Assert
        assertThat(targetVote).isNotNull();
        assertThat(targetVote.description()).isEqualTo(description);
        assertThat(targetVote.voteType()).isEqualTo(type);
        assertThat(targetVote.voteStatus()).isEqualTo(VoteStatus.OPEN);
        assertThat(targetVote.optionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("마감된 투표도 목록에 포함된다")
    void 마감된_투표도_목록에_포함된다(@Autowired UserFixture userFixture, @Autowired VoteFixture voteFixture) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        String title = "Expired Vote";
        voteFixture.createVote(title, "desc", VoteType.SINGLE, List.of("1", "2"), LocalDateTime.now().plusSeconds(5),
                token);

        // Act
        ApiResponse<VoteInfoResponse[]> response = voteFixture.getVoteInfos(token);

        // Assert
        VoteInfoResponse targetVote = findVoteByTitle(response.getData(), title);
        assertThat(targetVote).isNotNull();
    }

    private VoteInfoResponse findVoteByTitle(VoteInfoResponse[] votes, String title) {
        if (votes == null)
            return null;
        for (VoteInfoResponse vote : votes) {
            if (vote.title().equals(title)) {
                return vote;
            }
        }
        return null;
    }

}
