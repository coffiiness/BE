package com.coffiness.calfit.api.recruitment.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.CareerType;
import com.coffiness.calfit.core.enums.RecruitmentStageType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/recruitments")
public class GET_specs {

  @Test
  void 채용공고_목록_조회에_성공한다(
      @Autowired UserFixture userFixture, @Autowired RecruitmentFixture recruitmentFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();

    List<RecruitmentStageRequest> stages1 =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

    List<RecruitmentStageRequest> stages2 =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("처우 협의", RecruitmentStageType.OFFER, 3),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 4));

    RecruitmentCreateRequest request1 =
        new RecruitmentCreateRequest(
            "시니어 백엔드 채용",
            2,
            1L,
            "경력직 모집합니다",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.EXPERIENCED,
            5,
            10,
            1L,
            List.of(1L, 2L),
            List.of(3L, 4L),
            stages1);
    RecruitmentCreateRequest request2 =
        new RecruitmentCreateRequest(
            "신입 프론트엔드 채용",
            3,
            2L,
            "신입 모집합니다.",
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(30),
            CareerType.NEW,
            null,
            null,
            1L,
            List.of(),
            List.of(),
            stages2);

    recruitmentFixture.createRecruitment(token, request1);
    recruitmentFixture.createRecruitment(token, request2);

    // Act
    ApiResponse<List<RecruitmentListResponse>> response =
        recruitmentFixture.getRecruitmentList(token);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().size()).isEqualTo(2);
    assertThat(response.getData().get(0).title()).contains("채용");
  }
}
