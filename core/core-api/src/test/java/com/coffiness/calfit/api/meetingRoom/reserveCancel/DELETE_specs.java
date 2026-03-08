package com.coffiness.calfit.api.meetingRoom.reserveCancel;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.MeetingRoomReservationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("DELETE /api/v1/meeting-rooms/{meetingRoomId}/reservations/{reservationId}")
class DELETE_specs {

  @Test
  void 예약_취소에_성공한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    ApiResponse<MeetingRoomReservationResponse> reservation =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 0),
            LocalDateTime.of(2030, 1, 10, 11, 0));

    ApiResponse<MeetingRoomReservationResponse> canceled =
        meetingRoomFixture.cancelReservation(
            token, tenantId, meetingRoomId, reservation.getData().id());

    assertThat(canceled.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(canceled.getData().status()).isEqualTo(MeetingRoomStatus.DELETED);
  }

  @Test
  void 존재하지_않는_예약_취소시_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    ApiResponse<MeetingRoomReservationResponse> canceled =
        meetingRoomFixture.cancelReservation(token, tenantId, meetingRoomId, 99999L);

    assertThat(canceled.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 토큰이_없으면_예약_취소에_실패한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    ApiResponse<MeetingRoomReservationResponse> reservation =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 0),
            LocalDateTime.of(2030, 1, 10, 11, 0));

    ApiResponse<MeetingRoomReservationResponse> canceled =
        meetingRoomFixture.cancelReservation(
            null, tenantId, meetingRoomId, reservation.getData().id());

    assertThat(canceled.getResult()).isEqualTo(ResultType.ERROR);
  }
}
