package com.coffiness.calfit.domain.announcementBoard;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementBoardService {

  private final AnnouncementBoardReader announcementBoardReader;
  private final AnnouncementBoardStore announcementBoardStore;
  private final AnnouncementBoardValidator announcementBoardValidator;

  /* 공지사항 생성 */
  @Transactional
  public AnnouncementBoard create(String title, String content, Boolean pinned, Long userId) {
    announcementBoardValidator.validateCreateRequest(title, content, pinned, userId);
    return announcementBoardStore.create(title, content, pinned);
  }

  /* 공지사항 조회 */
  @Transactional(readOnly = true)
  public List<AnnouncementBoard> list() {
    return announcementBoardReader.getAnnouncementBoards();
  }

  /* 공지사항 수정 */
  @Transactional
  public AnnouncementBoard update(
      Long id, String title, String content, Boolean pinned, Long userId) {
    announcementBoardValidator.validateUpdateRequest(id, title, content, pinned, userId);
    return announcementBoardStore.update(id, title, content, pinned);
  }

  /* 공지사항 삭제 */
  @Transactional
  public void delete(Long id, Long userId) {
    announcementBoardValidator.validateDeleteRequest(id, userId);
    announcementBoardStore.delete(id);
  }
}
