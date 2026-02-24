package com.coffiness.calfit.domain.workspace.invitation;

import com.coffiness.calfit.core.enums.InvitationStatus;
import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.storage.db.core.invitation.WorkspaceInvitationEntity;
import com.coffiness.calfit.storage.db.core.invitation.WorkspaceInvitationRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvitationService {
  private static final long EXPIRATION_HOURS = 72;
  private final WorkspaceInvitationRepository invitationRepository;

  @Transactional
  public Invitation createInvitation(
      String workspaceId, String email, MemberType memberType, Long invitedBy) {
    if (invitationRepository.existsByWorkspaceIdAndEmailAndInvitationStatus(
        workspaceId, email, InvitationStatus.PENDING)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    String token = UUID.randomUUID().toString();
    LocalDateTime expiresAt = LocalDateTime.now().plusHours(EXPIRATION_HOURS);

    WorkspaceInvitationEntity entity =
        WorkspaceInvitationEntity.create(
            workspaceId, email, token, memberType, invitedBy, expiresAt);
    WorkspaceInvitationEntity saved = invitationRepository.save(entity);

    return toInvitation(saved);
  }

  @Transactional(readOnly = true)
  public Invitation getByToken(String token) {
    WorkspaceInvitationEntity entity =
        invitationRepository
            .findByToken(token)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    if (entity.isExpired() || entity.getInvitationStatus() != InvitationStatus.PENDING) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }

    return toInvitation(entity);
  }

  @Transactional
  public void acceptInvitation(String token) {
    WorkspaceInvitationEntity entity =
        invitationRepository
            .findByToken(token)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    entity.accept();
  }

  private Invitation toInvitation(WorkspaceInvitationEntity entity) {
    return new Invitation(
        entity.getId(),
        entity.getWorkspaceId(),
        entity.getEmail(),
        entity.getToken(),
        entity.getMemberType(),
        entity.getInvitedBy(),
        entity.getInvitationStatus(),
        entity.getExpiresAt());
  }
}
