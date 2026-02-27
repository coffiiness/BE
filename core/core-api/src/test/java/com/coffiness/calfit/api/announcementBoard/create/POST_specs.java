package com.coffiness.calfit.api.announcementBoard.create;

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
@DisplayName("POST /api/v1/announcement-boards")
class POST_specs {

  @Test
  void 정상_요청시_공지사항이_생성된다(
      @Autowired UserFixture userFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, "공지 제목", "공지 내용", true);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(response.getData()).isNotNull();
    assertThat(response.getData().id()).isNotNull();
    assertThat(response.getData().pinned()).isTrue();
  }

  @Test
  void 인증_토큰이_없으면_401을_반환한다(@Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create("", "공지 제목", "공지 내용", false);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void 제목이_없으면_400을_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, null, "공지 내용", false);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }

  @Test
  void pinned_값이_없으면_400을_반환한다(
      @Autowired UserFixture userFixture,
      @Autowired AnnouncementBoardFixture announcementBoardFixture) {
    // Arrange
    String token = userFixture.createUserAndGetToken();

    // Act
    ApiResponse<AnnouncementBoardResponse> response =
        announcementBoardFixture.create(token, "공지 제목", "공지 내용", null);

    // Assert
    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
