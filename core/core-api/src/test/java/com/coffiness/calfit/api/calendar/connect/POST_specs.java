package com.coffiness.calfit.api.calendar.connect;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.model.GoogleTokenModel;
import com.coffiness.calfit.port.GoogleOAuthPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@CalfitApiTest
@DisplayName("POST /api/v1/calendars/google/connect")
public class POST_specs {

    @MockitoBean
    private GoogleOAuthPort googleOAuthPort;

    @Test
    void 올바른_인가코드로_요청하면_캘린더_연동에_성공한다(
            @Autowired UserFixture userFixture,
            @Autowired CalendarFixture calendarFixture
    ) {
        String token = userFixture.createUserAndGetToken();
        String validAuthCode = "dummy-auth-code";

        given(googleOAuthPort.exchangeToken(validAuthCode))
                .willReturn(new GoogleTokenModel("mock-access", "mock-refresh", 3600, "test@gmail.com"));

        // Act
        ApiResponse<Void> response = calendarFixture.connectGoogleCalendar(token, validAuthCode);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    }

    @Test
    void 인가코드가_비어있으면_에러_응답을_반환한다(
            @Autowired UserFixture userFixture,
            @Autowired CalendarFixture calendarFixture
    ) {
        // Arrange
        String token = userFixture.createUserAndGetToken();
        String emptyAuthCode = "";

        // Act
        ApiResponse<Void> response = calendarFixture.connectGoogleCalendar(token, emptyAuthCode);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

}
