package com.coffiness.calfit.storage.db.core.announcementBoard;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementBoardRepository extends JpaRepository<AnnouncementBoardEntity, Long> {

  boolean existsByTitle(String title);

  boolean existsByTitleAndStatus(String title, EntityStatus status);

  Optional<AnnouncementBoardEntity> findByTitle(String title);

  Optional<AnnouncementBoardEntity> findByTitleAndStatus(String title, EntityStatus status);

  Optional<AnnouncementBoardEntity> findByIdAndStatus(Long id, EntityStatus status);

  List<AnnouncementBoardEntity> findAllByOrderByPinnedDescCreatedAtDesc();

  List<AnnouncementBoardEntity> findAllByStatusOrderByPinnedDescCreatedAtDesc(EntityStatus status);
}
