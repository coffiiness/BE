package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;

public record AnnouncementBoardResponse(Long id, String title, String content, Boolean pinned) {

  public static AnnouncementBoardResponse from(AnnouncementBoard announcementBoard) {
    return new AnnouncementBoardResponse(
        announcementBoard.id(),
        announcementBoard.title(),
        announcementBoard.content(),
        announcementBoard.pinned());
  }
}
