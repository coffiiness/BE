package com.coffiness.calfit.domain.announcementBoard;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.announcementBoard.AnnouncementBoardEntity;
import com.coffiness.calfit.storage.db.core.announcementBoard.AnnouncementBoardRepository;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnnouncementBoardService {

  private final AnnouncementBoardRepository announcementBoardRepository;
  private final MemberReader memberReader;

  /* 공지사항 생성 */
  @Transactional
  public AnnouncementBoard create(String title, String content, Boolean pinned, Long userId) {
    String tenantId = TenantContext.getTenantId();
    assertHrMember(tenantId, userId);

    if (announcementBoardRepository.existsByTitle(title)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    AnnouncementBoardEntity entity =
        AnnouncementBoardEntity.builder().title(title).content(content).pinned(pinned).build();

    try {
      AnnouncementBoardEntity saved = announcementBoardRepository.save(entity);
      return toAnnouncementBoard(saved);
    } catch (DataIntegrityViolationException e) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  /* 공지사항 조회 */
  @Transactional(readOnly = true)
  public List<AnnouncementBoard> list() {
    List<AnnouncementBoardEntity> entities =
        announcementBoardRepository.findAllByOrderByPinnedDescCreatedAtDesc();

    List<AnnouncementBoard> result = new ArrayList<>();
    for (AnnouncementBoardEntity entity : entities) {
      result.add(toAnnouncementBoard(entity));
    }

    return result;
  }

  /* 공지사항 수정 */
  @Transactional
  public AnnouncementBoard update(
      Long id, String title, String content, Boolean pinned, Long userId) {
    String tenantId = TenantContext.getTenantId();
    assertHrMember(tenantId, userId);

    AnnouncementBoardEntity entity =
        announcementBoardRepository
            .findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    Optional<AnnouncementBoardEntity> duplicated = announcementBoardRepository.findByTitle(title);
    if (duplicated.isPresent() && !duplicated.get().getId().equals(id)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    entity.update(title, content, pinned);
    return toAnnouncementBoard(entity);
  }

  /* 공지사항 삭제 */
  @Transactional
  public void delete(Long id, Long userId) {
    String tenantId = TenantContext.getTenantId();
    assertHrMember(tenantId, userId);

    AnnouncementBoardEntity entity =
        announcementBoardRepository
            .findById(id)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    announcementBoardRepository.delete(entity);
  }

  private AnnouncementBoard toAnnouncementBoard(AnnouncementBoardEntity entity) {
    return new AnnouncementBoard(
        entity.getId(), entity.getTitle(), entity.getContent(), entity.getPinned());
  }

  private void assertHrMember(String tenantId, Long userId) {
    if (tenantId == null || userId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }

    Member member = memberReader.getMember(tenantId, userId);
    if (member == null || member.memberType() != MemberType.HR) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }
}
