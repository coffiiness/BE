package com.coffiness.calfit.api.calendar.update;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.request.ScheduleUpdateRequest;
import com.coffiness.calfit.response.ScheduleDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@CalfitApiTest
@DisplayName("PUT /api/v1/schedules/{scheduleId}")
public class PUT_specs {

    @Test
    void 특정_일정의_정보를_수정할_수_있다(
            @Autowired UserFixture userFixture,
            @Autowired CalendarFixture calendarFixture
            ) {

        // Arrange
        String token = userFixture.createUserAndGetToken();
        LocalDateTime now = LocalDateTime.now();

        ScheduleCreateRequest createRequest = new ScheduleCreateRequest(
                "수정용 회의",
                "수정용 회의 description",
                ScheduleType.MEETING,
                now.plusDays(2),
                now.plusDays(2).plusHours(2),
                false,
                null,
                false,
                null
        );
        calendarFixture.createSchedule(token, createRequest);

        String startDate = now.toLocalDate().toString();
        String endDate = now.toLocalDate().plusDays(5).toString();
        Long scheduleId = calendarFixture.getSchedules(token, startDate, endDate).getData().get(0).id();

        ScheduleUpdateRequest updateRequest = new ScheduleUpdateRequest(
                "수정된 제목",
                "수정된 Description",
                ScheduleType.BUSINESS,
                now.plusDays(3),
                now.plusDays(3).plusHours(5),
                false,
                null,
                false,
                null
        );

        // Act
        ApiResponse<Void> updateResponse = calendarFixture.updateSchedule(token, scheduleId, updateRequest);

        // Assert
        assertThat(updateResponse.getResult()).isEqualTo(ResultType.SUCCESS);

        ApiResponse<ScheduleDetailResponse> detailResponse = calendarFixture.getDetailSchedule(token, scheduleId);

        assertThat(detailResponse.getData().title()).isEqualTo("수정된 제목");
        assertThat(detailResponse.getData().description()).isEqualTo("수정된 Description");
        assertThat(detailResponse.getData().type()).isEqualTo(ScheduleType.BUSINESS);

    }
}
