package com.coffiness.calfit.storage.db.core.announcementBoard;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementBoardRepository extends JpaRepository<AnnouncementBoardEntity, Long> {

  boolean existsByTitle(String title);

  Optional<AnnouncementBoardEntity> findByTitle(String title);

  List<AnnouncementBoardEntity> findAllByOrderByPinnedDescCreatedAtDesc();
}
