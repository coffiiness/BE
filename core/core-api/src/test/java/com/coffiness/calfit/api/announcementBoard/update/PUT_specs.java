package com.coffiness.calfit.api.announcementBoard.update;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.AnnouncementBoardFixture;
import com.coffiness.calfit.api.fixture.UserFixture;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@CalfitApiTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@DisplayName("PUT /api/v1/announcement-boards/{boardId}")
class PUT_specs {

  @Test
  void 올바른_요청_시_200_OK를_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long boardId = createBoardAndGetId(announcementBoardFixture, token, "공지 A", "내용 A", false);

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, boardId, "공지 B", "내용 B", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().pinned()).isTrue();
  }

  @Test
  void 토큰이_없으면_401을_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long boardId = createBoardAndGetId(announcementBoardFixture, token, "공지 A", "내용 A", false);

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(null, boardId, "공지 B", "내용 B", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 존재하지_않는_boardId이면_에러를_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, 99999L, "공지 B", "내용 B", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void pinned_값이_없으면_400을_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();
    long boardId = createBoardAndGetId(announcementBoardFixture, token, "공지 A", "내용 A", false);

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.update(token, boardId, "공지 B", "내용 B", null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  private long createBoardAndGetId(
      AnnouncementBoardFixture announcementBoardFixture,
      String token,
      String title,
      String content,
      Boolean pinned) {
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, title, content, pinned);
    return response.getData().id();
  }
}
