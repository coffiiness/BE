package com.coffiness.calfit.api.calendar.create;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("POST /api/v1/schedules")
public class POST_specs {

  @Test
  void 필수_데이터를_입력하면_내_일정_생성에_성공한다(
      @Autowired UserFixture userFixture, @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "기획 회의",
            "프로젝트 초기 기획 회의입니다.",
            ScheduleType.MEETING,
            now.plusDays(1),
            now.plusDays(1).plusHours(1),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> response = calendarFixture.createSchedule(token, createRequest);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }
}
