package com.coffiness.calfit.api.calendar.detail;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.response.ScheduleDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@CalfitApiTest
@DisplayName("GET /api/v1/schedules/{scheduleId}")
public class GET_specs {

    @Test
    void 특정_일정의_상세_정보를_조회할_수_있다 (
            @Autowired UserFixture userFixture,
            @Autowired CalendarFixture calendarFixture) {

        // Arrange
        String token = userFixture.createUserAndGetToken();

        LocalDateTime now = LocalDateTime.now();
        ScheduleCreateRequest createRequest = new ScheduleCreateRequest(
                "상세 조회용 테스트 회의",
                "일정 상세 조회 테스트 description",
                ScheduleType.MEETING,
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                false,
                null,
                false
        );
        calendarFixture.createSchedule(token, createRequest);

        // Act
        String startDate = now.toLocalDate().toString();
        String endDate = now.toLocalDate().plusDays(5).toString();
        Long scheduleId = calendarFixture.getSchedules(token, startDate, endDate).getData().get(0).id();

        ApiResponse<ScheduleDetailResponse> response = calendarFixture.getDetailSchedule(token, scheduleId);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().title()).isEqualTo("상세 조회용 테스트 회의");

    }
}
