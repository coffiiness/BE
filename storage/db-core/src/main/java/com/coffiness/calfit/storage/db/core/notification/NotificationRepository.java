package com.coffiness.calfit.storage.db.core.notification;

import com.coffiness.calfit.core.enums.EntityStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

  List<NotificationEntity> findByTenantIdAndRecipientUserIdAndStatusOrderByCreatedAtDesc(
      String tenantId, Long recipientUserId, EntityStatus status, Pageable pageable);

  Optional<NotificationEntity> findByTenantIdAndRecipientUserIdAndIdAndStatus(
      String tenantId, Long recipientUserId, Long id, EntityStatus status);

  List<NotificationEntity> findByTenantIdAndRecipientUserIdAndStatusAndIsReadFalseOrderByCreatedAtDesc(
      String tenantId, Long recipientUserId, EntityStatus status);
}
