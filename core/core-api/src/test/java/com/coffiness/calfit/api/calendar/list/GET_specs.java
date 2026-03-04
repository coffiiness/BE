package com.coffiness.calfit.api.calendar.list;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.response.ScheduleResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/schedules")
public class GET_specs {

  @Test
  void 특정_기간의_일정을_조회하면_리스트를_반환한다(
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
            List.of(1L, 2L));

    ApiResponse<Void> createResponse = calendarFixture.createSchedule(token, createRequest);
    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    // Act
    String startDate = now.toLocalDate().minusDays(1).toString();
    String endDate = now.toLocalDate().plusDays(7).toString();
    ApiResponse<List<ScheduleResponse>> response =
        calendarFixture.getSchedules(token, startDate, endDate);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().size()).isGreaterThan(0);
    assertThat(response.getData().get(0).title()).isEqualTo("기획 회의");
  }
}
