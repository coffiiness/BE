package com.coffiness.calfit.api.calendar.detail;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.response.ScheduleDetailResponse;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("GET /api/v1/schedules/{scheduleId}")
public class GET_specs {

  @Test
  void 특정_일정의_상세_정보를_조회할_수_있다(
      @Autowired UserFixture userFixture, @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "상세 조회용 테스트 회의",
            "일정 상세 조회 테스트 description",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            null);
    calendarFixture.createSchedule(token, createRequest);

    // Act
    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    Long scheduleId = calendarFixture.getSchedules(token, startDate, endDate).getData().get(0).id();

    ApiResponse<ScheduleDetailResponse> response =
        calendarFixture.getDetailSchedule(token, scheduleId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().title()).isEqualTo("상세 조회용 테스트 회의");
  }

  @Test
  void 특정_일정의_단건_상세_정보를_조회한다(
      @Autowired UserFixture userFixture, @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();

    Long myUserId = 1L;

    LocalDateTime now = LocalDateTime.now();
    ScheduleCreateRequest request =
        new ScheduleCreateRequest(
            "상세 조회용 참석자 테스트",
            "참석자가 잘 들어가는지 테스트.",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            List.of(myUserId, 999L));
    calendarFixture.createSchedule(token, request);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    Long targetScheduleId =
        calendarFixture.getSchedules(token, startDate, endDate).getData().get(0).id();

    // Act
    ApiResponse<ScheduleDetailResponse> response =
        calendarFixture.getDetailSchedule(token, targetScheduleId);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().title()).isEqualTo("상세 조회용 참석자 테스트");
    assertThat(response.getData().attendeeIds()).hasSize(2);
    assertThat(response.getData().attendeeIds()).contains(myUserId, 999L);
    assertThat(response.getData().attendees()).hasSize(2);
  }
}
