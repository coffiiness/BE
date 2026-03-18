package com.coffiness.calfit.api.notification.read;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.NotificationResponse;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("PATCH /api/v1/notifications")
class PATCH_specs {

  @Test
  void 알림을_읽음처리하고_전체읽음처리할_수_있다(
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

    notificationFixture.readAll(token, tenantId);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(notificationFixture.unreadCount(token, tenantId).getData().unreadCount())
                    .isZero());
  }

  @Test
  void 단건_읽음처리하면_unread_count가_감소한다(
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

    long beforeCount = notificationFixture.unreadCount(token, tenantId).getData().unreadCount();
    NotificationResponse firstNotification =
        notificationFixture.unread(token, tenantId, null, 0, 10).getData().contents().get(0);

    ApiResponse<NotificationResponse> response =
        notificationFixture.read(token, tenantId, firstNotification.id());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData().isRead()).isTrue();

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(notificationFixture.unreadCount(token, tenantId).getData().unreadCount())
                    .isEqualTo(beforeCount - 1));
  }

  @Test
  void 다른_사용자의_알림은_읽음처리할_수_없다(
      @Autowired MemberFixture memberFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture,
      @Autowired NotificationFixture notificationFixture,
      @Autowired UserFixture userFixture) {
    MemberFixture.WorkspaceContext context = memberFixture.setupWorkspace();
    String ownerToken = context.hrToken();
    String tenantId = context.workspaceId();

    announcementBoardFixture.create(ownerToken, tenantId, "공지 1", "내용 1", false);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(notificationFixture.unreadCount(ownerToken, tenantId).getData().unreadCount())
                    .isGreaterThanOrEqualTo(1));

    NotificationResponse ownerNotification =
        notificationFixture.unread(ownerToken, tenantId, null, 0, 10).getData().contents().get(0);

    String otherEmail = userFixture.randomEmail();
    String otherPassword = userFixture.randomPassword();
    userFixture.signUp(otherEmail, otherPassword, "다른 사용자");
    String otherToken = userFixture.login(otherEmail, otherPassword).getData().accessToken();
    var invitationResponse =
        memberFixture.createInvitation(ownerToken, tenantId, otherEmail, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), otherToken);

    ApiResponse<NotificationResponse> response =
        notificationFixture.read(otherToken, tenantId, ownerNotification.id());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(response.getError()).isNotNull();
    assertThat(notificationFixture.unread(ownerToken, tenantId, null, 0, 10).getData().contents())
        .extracting(NotificationResponse::id)
        .contains(ownerNotification.id());
  }
}
