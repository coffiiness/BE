package com.coffiness.calfit.api.meetingRoom.reserve;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.v1.response.MeetingRoomReservationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("POST /api/v1/meeting-rooms/{meetingRoomId}/reservations")
class POST_specs {

  @Test
  void 정상_요청시_회의실_예약이_생성된다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);
    LocalDateTime end = LocalDateTime.of(2030, 1, 10, 11, 0);

    ApiResponse<MeetingRoomReservationResponse> response =
        meetingRoomFixture.reserve(token, tenantId, meetingRoomId, start, end);

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().meetingRoomId()).isEqualTo(meetingRoomId);
  }

  @Test
  void 동일_시간대_중복_예약이면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    LocalDateTime start = LocalDateTime.of(2030, 1, 10, 10, 0);
    LocalDateTime end = LocalDateTime.of(2030, 1, 10, 11, 0);

    ApiResponse<MeetingRoomReservationResponse> first =
        meetingRoomFixture.reserve(token, tenantId, meetingRoomId, start, end);
    assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomReservationResponse> second =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 30),
            LocalDateTime.of(2030, 1, 10, 11, 30));

    assertThat(second.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 연속된_다음_시간대_예약은_성공한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    ApiResponse<MeetingRoomReservationResponse> first =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 10, 0),
            LocalDateTime.of(2030, 1, 10, 11, 0));
    assertThat(first.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<MeetingRoomReservationResponse> second =
        meetingRoomFixture.reserve(
            token,
            tenantId,
            meetingRoomId,
            LocalDateTime.of(2030, 1, 10, 11, 0),
            LocalDateTime.of(2030, 1, 10, 12, 0));

    assertThat(second.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 시작시간과_종료시간이_같으면_에러를_반환한다(
      @Autowired MemberFixture memberFixture, @Autowired MeetingRoomFixture meetingRoomFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    ApiResponse<MeetingRoomResponse> room =
        meetingRoomFixture.create(token, tenantId, "회의실 A", 1, 6);
    Long meetingRoomId = room.getData().id();

    LocalDateTime sameTime = LocalDateTime.of(2030, 1, 10, 10, 0);
    ApiResponse<MeetingRoomReservationResponse> response =
        meetingRoomFixture.reserve(token, tenantId, meetingRoomId, sameTime, sameTime);

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
