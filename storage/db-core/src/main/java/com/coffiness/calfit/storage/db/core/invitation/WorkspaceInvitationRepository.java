package com.coffiness.calfit.storage.db.core.invitation;

import com.coffiness.calfit.core.enums.InvitationStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceInvitationRepository
    extends JpaRepository<WorkspaceInvitationEntity, Long> {

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query(
      "select i from WorkspaceInvitationEntity i where i.token = :token")
  Optional<WorkspaceInvitationEntity> findByTokenForUpdate(
      @org.springframework.data.repository.query.Param("token") String token);

  Optional<WorkspaceInvitationEntity> findByToken(String token);

  boolean existsByWorkspaceIdAndEmailAndInvitationStatus(
      String workspaceId, String email, InvitationStatus invitationStatus);
}
