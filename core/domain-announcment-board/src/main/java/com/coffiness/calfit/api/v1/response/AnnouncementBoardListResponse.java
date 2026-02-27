package com.coffiness.calfit.api.v1.response;

import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;

public record AnnouncementBoardListResponse(Long id, String title, String content, Boolean pinned) {

  public static AnnouncementBoardListResponse from(AnnouncementBoard announcementBoard) {
    return new AnnouncementBoardListResponse(
        announcementBoard.id(),
        announcementBoard.title(),
        announcementBoard.content(),
        announcementBoard.pinned());
  }
}
