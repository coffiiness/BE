package com.coffiness.calfit.api.meetingRoom.create;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("POST /api/v1/meeting-rooms")
class POST_specs {

    @Test
    void 정상_요청시_회의실이_생성된다(
            @Autowired UserFixture userFixture,
            @Autowired MeetingRoomFixture meetingRoomFixture
    ) {

        // Arrange
        String token = userFixture.createUserAndGetToken();
        String name = "회의실 A";
        Integer location = 1;
        Integer capacity = 10;

        // Act
        ApiResponse<MeetingRoomResponse> response =
                meetingRoomFixture.create(token, name, location, capacity);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().id()).isNotNull();
    }

    @Test
    void 인증_토큰이_없으면_401을_반환한다(
            @Autowired MeetingRoomFixture meetingRoomFixture
    ) {

        // Arrange
        String token = "";
        String name = "회의실 A";
        Integer location = 1;
        Integer capacity = 10;

        // Act
        ApiResponse<MeetingRoomResponse> response =
                meetingRoomFixture.create(token, name, location, capacity);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 이름이_없으면_400을_반환한다(
            @Autowired UserFixture userFixture,
            @Autowired MeetingRoomFixture meetingRoomFixture
    ) {

        // Arrange
        String token = userFixture.createUserAndGetToken();
        String name = null;
        Integer location = 1;
        Integer capacity = 10;

        // Act
        ApiResponse<MeetingRoomResponse> response =
                meetingRoomFixture.create(token, name, location, capacity);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 이름이_2자_미만이면_400을_반환한다(
            @Autowired UserFixture userFixture,
            @Autowired MeetingRoomFixture meetingRoomFixture
    ) {

        // Arrange
        String token = userFixture.createUserAndGetToken();
        String name = "A";
        Integer location = 1;
        Integer capacity = 10;

        // Act
        ApiResponse<MeetingRoomResponse> response =
                meetingRoomFixture.create(token, name, location, capacity);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 이름이_50자를_초과하면_400을_반환한다(
            @Autowired UserFixture userFixture,
            @Autowired MeetingRoomFixture meetingRoomFixture
    ) {

        // Arrange
        String token = userFixture.createUserAndGetToken();
        String name = "a".repeat(51);
        Integer location = 1;
        Integer capacity = 10;

        // Act
        ApiResponse<MeetingRoomResponse> response =
                meetingRoomFixture.create(token, name, location, capacity);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 수용인원이_없으면_400을_반환한다(
            @Autowired UserFixture userFixture,
            @Autowired MeetingRoomFixture meetingRoomFixture
    ) {

        // Arrange
        String token = userFixture.createUserAndGetToken();
        String name = "회의실 A";
        Integer location = 1;
        Integer capacity = null;

        // Act
        ApiResponse<MeetingRoomResponse> response =
                meetingRoomFixture.create(token, name, location, capacity);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 수용인원이_1미만이면_400을_반환한다(
            @Autowired UserFixture userFixture,
            @Autowired MeetingRoomFixture meetingRoomFixture
    ) {

        // Arrange
        String token = userFixture.createUserAndGetToken();
        String name = "회의실 A";
        Integer location = 1;
        Integer capacity = 0;

        // Act
        ApiResponse<MeetingRoomResponse> response =
                meetingRoomFixture.create(token, name, location, capacity);

        // Assert
        assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    }

    @Test
    void 같은_워크스페이스_내_중복된_이름이면_409를_반환한다(
            @Autowired UserFixture userFixture,
            @Autowired MeetingRoomFixture meetingRoomFixture
    ) {

        // Arrange
        String token = userFixture.createUserAndGetToken();
        String name = "회의실 A";
        Integer location = 1;
        Integer capacity = 10;

        ApiResponse<MeetingRoomResponse> first =
                meetingRoomFixture.create(token, name, location, capacity);

        assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

        // Act
        ApiResponse<MeetingRoomResponse> second =
                meetingRoomFixture.create(token, name, location, capacity);

        // Assert
        assertThat(second.getResult()).isEqualTo(ResultType.ERROR);
    }
}
