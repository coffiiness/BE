package com.coffiness.calfit.api.calendar.delete;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("DELETE /api/v1/schedules/{scheduleId}")
public class DELETE_specs {

  @Test
  void 특정_일정을_삭제할_수_있다(
      @Autowired UserFixture userFixture, @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    LocalDateTime now = LocalDateTime.now();

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "삭제용 일정",
            "삭제용 일정",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            null);
    calendarFixture.createSchedule(token, createRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();

    Long scheduleId = calendarFixture.getSchedules(token, startDate, endDate).getData().get(0).id();

    // Act
    ApiResponse<Void> deleteSchedule = calendarFixture.deleteSchedule(token, scheduleId);

    // Assert
    assertThat(deleteSchedule.getResult()).isEqualTo(ResultType.SUCCESS);

    int sizeAfterDelete = calendarFixture.getSchedules(token, startDate, endDate).getData().size();
    assertThat(sizeAfterDelete).isEqualTo(0);
  }
}
