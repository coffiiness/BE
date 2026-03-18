package com.coffiness.calfit.api.notification.list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.CalendarFixture;
import com.coffiness.calfit.api.fixture.MeetingRoomFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture.NotificationPageResponse;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.InvitationResponse;
import com.coffiness.calfit.api.v1.response.NotificationResponse;
import com.coffiness.calfit.api.v1.response.NotificationUnreadCountResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.enums.NotificationType;
import com.coffiness.calfit.core.enums.ScheduleType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.v1.request.ScheduleCreateRequest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("GET /api/v1/notifications")
class GET_specs {

  @Test
  void 공지사항_생성시_알림_목록과_미확인_개수가_조회된다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture,
      @Autowired NotificationFixture notificationFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", false);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              ApiResponse<NotificationUnreadCountResponse> unreadCountResponse =
                  notificationFixture.unreadCount(token, tenantId);
              assertThat(unreadCountResponse.getResult()).isEqualTo(ResultType.SUCCESS);
              assertThat(unreadCountResponse.getData().unreadCount()).isGreaterThanOrEqualTo(1);
            });

    ApiResponse<NotificationPageResponse> unreadResponse =
        notificationFixture.unread(token, tenantId, "ANNOUNCEMENT", 0, 5);

    assertThat(unreadResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(unreadResponse.getData().contents()).isNotEmpty();
    assertThat(unreadResponse.getData().contents().get(0).type().name())
        .isEqualTo("ANNOUNCEMENT_CREATED");
  }

  @Test
  void 타입_필터로_원하는_알림만_조회한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture,
      @Autowired MeetingRoomFixture meetingRoomFixture,
      @Autowired NotificationFixture notificationFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    announcementBoardFixture.create(token, tenantId, "공지 제목", "공지 내용", false);
    Long meetingRoomId =
        meetingRoomFixture.create(token, tenantId, "알림 필터 회의실", 3, 6).getData().id();
    meetingRoomFixture.reserve(
        token,
        tenantId,
        meetingRoomId,
        LocalDateTime.of(2030, 4, 10, 10, 0),
        LocalDateTime.of(2030, 4, 10, 11, 0));

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(
                        notificationFixture.list(token, tenantId, null, 0, 10).getData().contents())
                    .hasSizeGreaterThanOrEqualTo(2));

    ApiResponse<NotificationPageResponse> announcementResponse =
        notificationFixture.list(token, tenantId, "ANNOUNCEMENT", 0, 10);
    ApiResponse<NotificationPageResponse> meetingRoomResponse =
        notificationFixture.list(token, tenantId, "MEETING_ROOM", 0, 10);

    assertThat(announcementResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(announcementResponse.getData().contents())
        .extracting(NotificationResponse::type)
        .containsOnly(NotificationType.ANNOUNCEMENT_CREATED);

    assertThat(meetingRoomResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(meetingRoomResponse.getData().contents())
        .extracting(NotificationResponse::type)
        .containsOnly(NotificationType.MEETING_ROOM_RESERVED);
  }

  @Test
  void 읽지_않은_알림만_별도로_조회한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture,
      @Autowired NotificationFixture notificationFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    announcementBoardFixture.create(token, tenantId, "공지 1", "내용 1", false);
    announcementBoardFixture.create(token, tenantId, "공지 2", "내용 2", false);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(notificationFixture.unreadCount(token, tenantId).getData().unreadCount())
                    .isGreaterThanOrEqualTo(2));

    NotificationResponse firstNotification =
        notificationFixture.unread(token, tenantId, null, 0, 10).getData().contents().get(0);
    ApiResponse<NotificationResponse> readResponse =
        notificationFixture.read(token, tenantId, firstNotification.id());

    assertThat(readResponse.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(readResponse.getData().isRead()).isTrue();

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              List<NotificationResponse> unreadNotifications =
                  notificationFixture.unread(token, tenantId, null, 0, 10).getData().contents();
              List<NotificationResponse> allNotifications =
                  notificationFixture.list(token, tenantId, null, 0, 10).getData().contents();

              assertThat(unreadNotifications)
                  .allSatisfy(notification -> assertThat(notification.isRead()).isFalse());
              assertThat(unreadNotifications).hasSize(allNotifications.size() - 1);
              assertThat(unreadNotifications)
                  .extracting(NotificationResponse::id)
                  .doesNotContain(firstNotification.id());
            });
  }

  @Test
  void 페이지네이션_파라미터에_맞춰_알림을_나눠서_조회한다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture,
      @Autowired NotificationFixture notificationFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String token = context.hrToken();
    String tenantId = context.workspaceId();

    announcementBoardFixture.create(token, tenantId, "공지 A", "내용 A", false);
    announcementBoardFixture.create(token, tenantId, "공지 B", "내용 B", false);
    announcementBoardFixture.create(token, tenantId, "공지 C", "내용 C", false);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(
                        notificationFixture.list(token, tenantId, null, 0, 10).getData().contents())
                    .hasSizeGreaterThanOrEqualTo(3));

    ApiResponse<NotificationPageResponse> firstPage =
        notificationFixture.list(token, tenantId, null, 0, 2);
    ApiResponse<NotificationPageResponse> secondPage =
        notificationFixture.list(token, tenantId, null, 1, 2);

    assertThat(firstPage.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(firstPage.getData().contents()).hasSize(2);
    assertThat(firstPage.getData().hasNext()).isTrue();

    assertThat(secondPage.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(secondPage.getData().contents()).isNotEmpty();
    assertThat(secondPage.getData().contents())
        .extracting(NotificationResponse::id)
        .doesNotContainAnyElementsOf(
            firstPage.getData().contents().stream().map(NotificationResponse::id).toList());
  }

  @Test
  void 다른_사용자에게_온_알림만_조회된다(
      @Autowired MemberFixture memberFixture,
      @Autowired UserFixture userFixture,
      @Autowired CalendarFixture calendarFixture,
      @Autowired NotificationFixture notificationFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String ownerToken = context.hrToken();
    String tenantId = context.workspaceId();

    String attendeeEmail = userFixture.randomEmail();
    String attendeePassword = userFixture.randomPassword();
    userFixture.signUp(attendeeEmail, attendeePassword, userFixture.randomName());
    String attendeeToken =
        userFixture.login(attendeeEmail, attendeePassword).getData().accessToken();
    ApiResponse<InvitationResponse> invitationResponse =
        memberFixture.createInvitation(ownerToken, tenantId, attendeeEmail, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), attendeeToken);
    Long attendeeUserId = userFixture.me(attendeeToken).getData().id();

    LocalDateTime startAt = LocalDateTime.of(2030, 5, 10, 14, 0);
    ApiResponse<Void> createResponse =
        calendarFixture.createSchedule(
            ownerToken,
            tenantId,
            new ScheduleCreateRequest(
                "참석자 초대 일정",
                "알림 수신자 분리 테스트",
                ScheduleType.MEETING,
                startAt,
                startAt.plusHours(1),
                false,
                null,
                true,
                List.of(attendeeUserId)));

    assertThat(createResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(
                        notificationFixture
                            .unreadCount(attendeeToken, tenantId)
                            .getData()
                            .unreadCount())
                    .isGreaterThanOrEqualTo(1));

    ApiResponse<NotificationPageResponse> ownerUnread =
        notificationFixture.unread(ownerToken, tenantId, "SCHEDULE", 0, 10);
    ApiResponse<NotificationPageResponse> attendeeUnread =
        notificationFixture.unread(attendeeToken, tenantId, "SCHEDULE", 0, 10);

    assertThat(ownerUnread.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(ownerUnread.getData().contents()).isEmpty();

    assertThat(attendeeUnread.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(attendeeUnread.getData().contents())
        .extracting(NotificationResponse::type)
        .containsOnly(NotificationType.SCHEDULE_INVITED);
  }
}
