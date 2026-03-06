package com.coffiness.calfit.api.calendar.update;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.fixture.WorkspaceFixture;
import com.coffiness.calfit.api.v1.response.WorkspaceResponse;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import com.coffiness.calfit.v1.request.ScheduleUpdateRequest;
import com.coffiness.calfit.v1.response.ScheduleDetailResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@CalfitApiTest
@DisplayName("PUT /api/v1/schedules/{scheduleId}")
public class PUT_specs {

  @Test
  void 특정_일정의_정보를_수정할_수_있다(
      @Autowired UserFixture userFixture,
      @Autowired WorkspaceFixture workspaceFixture,
      @Autowired CalendarFixture calendarFixture) {

    // Arrange
    String token = userFixture.createUserAndGetToken();
    WorkspaceResponse workspace = workspaceFixture.createWorkspace(token).getData();
    String tenantId = workspace.workspaceId();
    LocalDateTime now = LocalDateTime.now();

    ScheduleCreateRequest createRequest =
        new ScheduleCreateRequest(
            "수정용 회의",
            "수정용 회의 description",
            ScheduleType.MEETING,
            now.plusDays(2),
            now.plusDays(2).plusHours(2),
            false,
            null,
            false,
            null);
    calendarFixture.createSchedule(token, tenantId, createRequest);

    String startDate = now.toLocalDate().toString();
    String endDate = now.toLocalDate().plusDays(5).toString();
    Long scheduleId =
        calendarFixture.getSchedules(token, tenantId, startDate, endDate).getData().get(0).id();

    ScheduleUpdateRequest updateRequest =
        new ScheduleUpdateRequest(
            "수정된 제목",
            "수정된 Description",
            ScheduleType.BUSINESS,
            now.plusDays(3),
            now.plusDays(3).plusHours(5),
            false,
            null,
            false,
            null);

    // Act
    ApiResponse<Void> updateResponse =
        calendarFixture.updateSchedule(token, tenantId, scheduleId, updateRequest);

    // Assert
    assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<ScheduleDetailResponse> detailResponse =
        calendarFixture.getDetailSchedule(token, tenantId, scheduleId);

    assertThat(detailResponse.getData().title()).isEqualTo("수정된 제목");
    assertThat(detailResponse.getData().description()).isEqualTo("수정된 Description");
    assertThat(detailResponse.getData().type()).isEqualTo(ScheduleType.BUSINESS);
  }
}
