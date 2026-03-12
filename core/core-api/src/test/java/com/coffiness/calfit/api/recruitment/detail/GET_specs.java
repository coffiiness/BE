package com.coffiness.calfit.api.recruitment.detail;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.RecruitmentFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.request.RecruitmentStageRequest;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
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
@DisplayName("GET /api/v1/recruitments/{recruitmentId}")
public class GET_specs {

  @Test
  void 채용공고_상세_조회가_성공한다(
      @Autowired UserFixture userFixture, @Autowired RecruitmentFixture recruitmentFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();

    List<RecruitmentStageRequest> stages1 =
        List.of(
            new RecruitmentStageRequest("서류 전형", RecruitmentStageType.DOCUMENT, 1),
            new RecruitmentStageRequest("면접 전형", RecruitmentStageType.INTERVIEW, 2),
            new RecruitmentStageRequest("최종 합격", RecruitmentStageType.PASS, 3));

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

    recruitmentFixture.createRecruitment(token, request1);

    Long recruitmentId = recruitmentFixture.getRecruitmentList(token).getData().get(0).id();

    // Act
    ApiResponse<RecruitmentDetailResponse> response =
        recruitmentFixture.getRecruitmentDetail(token, recruitmentId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();

    RecruitmentDetailResponse data = response.getData();
    assertThat(data.id()).isEqualTo(recruitmentId);
    assertThat(data.title()).isEqualTo("시니어 백엔드 채용");
    assertThat(data.contents()).isEqualTo("경력직 모집합니다");
    assertThat(data.careerType()).isEqualTo(CareerType.EXPERIENCED);
    assertThat(data.targetCount()).isEqualTo(2);
    assertThat(data.applicationTemplateId()).isEqualTo(1L);
    assertThat(data.stages()).hasSize(4);
    assertThat(data.stages().get(0).stageName()).isEqualTo("서류 전형");
    assertThat(data.stages().get(0).stageType()).isEqualTo(RecruitmentStageType.DOCUMENT);
    assertThat(data.stages().get(1).stageName()).isEqualTo("면접 전형");
    assertThat(data.stages().get(1).stageType()).isEqualTo(RecruitmentStageType.INTERVIEW);
    assertThat(data.endDate()).isAfter(data.startDate());
  }
}
