package com.coffiness.calfit.api.notification.delete;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.MemberFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture;
import com.coffiness.calfit.api.fixture.NotificationFixture.NotificationPageResponse;
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
@DisplayName("DELETE /api/v1/notifications")
class DELETE_specs {

  @Test
  void 알림을_삭제하고_전체삭제할_수_있다(
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
                assertThat(notificationFixture.list(token, tenantId, null, 0, 10).getData().contents())
                    .hasSizeGreaterThanOrEqualTo(2));

    NotificationPageResponse listResponse =
        notificationFixture.list(token, tenantId, null, 0, 10).getData();
    Long notificationId = listResponse.contents().get(0).id();

    ApiResponse<Void> deleteResponse = notificationFixture.delete(token, tenantId, notificationId);
    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(
                        notificationFixture
                            .list(token, tenantId, null, 0, 10)
                            .getData()
                            .contents()
                            .stream()
                            .map(NotificationResponse::id))
                    .doesNotContain(notificationId));

    ApiResponse<Void> deleteAllResponse = notificationFixture.deleteAll(token, tenantId);
    assertThat(deleteAllResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () ->
                assertThat(notificationFixture.list(token, tenantId, null, 0, 10).getData().contents())
                    .isEmpty());
  }

  @Test
  void 전체_삭제하면_목록이_비고_unread_count도_0이_된다(
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

    ApiResponse<Void> deleteAllResponse = notificationFixture.deleteAll(token, tenantId);

    assertThat(deleteAllResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    await()
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              assertThat(notificationFixture.list(token, tenantId, null, 0, 10).getData().contents())
                  .isEmpty();
              assertThat(notificationFixture.unreadCount(token, tenantId).getData().unreadCount())
                  .isZero();
            });
  }

  @Test
  void 다른_사용자의_알림은_삭제할_수_없다(
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
                assertThat(notificationFixture.list(ownerToken, tenantId, null, 0, 10).getData().contents())
                    .isNotEmpty());

    Long notificationId =
        notificationFixture.list(ownerToken, tenantId, null, 0, 10).getData().contents().get(0).id();

    String otherEmail = userFixture.randomEmail();
    String otherPassword = userFixture.randomPassword();
    userFixture.signUp(otherEmail, otherPassword, "다른 사용자");
    String otherToken = userFixture.login(otherEmail, otherPassword).getData().accessToken();
    var invitationResponse =
        memberFixture.createInvitation(ownerToken, tenantId, otherEmail, MemberType.INTERVIEWER);
    memberFixture.acceptInvitation(invitationResponse.getData().token(), otherToken);

    ApiResponse<Void> deleteResponse = notificationFixture.delete(otherToken, tenantId, notificationId);

    assertThat(deleteResponse.getResult()).isEqualTo(ResultType.ERROR);
    assertThat(deleteResponse.getError()).isNotNull();
    assertThat(notificationFixture.list(ownerToken, tenantId, null, 0, 10).getData().contents())
        .extracting(NotificationResponse::id)
        .contains(notificationId);
  }
}
