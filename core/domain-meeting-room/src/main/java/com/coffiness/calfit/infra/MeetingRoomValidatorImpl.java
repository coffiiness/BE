package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReader;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomValidator;
import com.coffiness.calfit.domain.user.UserReader;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MeetingRoomValidatorImpl implements MeetingRoomValidator {

  private final MeetingRoomReader meetingRoomReader;
  private final MemberReader memberReader;
  private final UserReader userReader;

  @Override
  public void validateCreateInput(String name, Integer location, Integer capacity) {
    requireTenantId();
    if (name == null || name.isBlank() || location == null || capacity == null || capacity < 2) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (meetingRoomReader.existsByName(name)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateUpdateInput(Long meetingRoomId, String name, Integer capacity) {
    requireTenantId();
    if (meetingRoomId == null
        || name == null
        || name.isBlank()
        || capacity == null
        || capacity < 2) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    MeetingRoom current = meetingRoomReader.getMeetingRoom(meetingRoomId);
    if (!current.name().equals(name) && meetingRoomReader.existsByName(name)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateDeleteRequest(Long meetingRoomId, Long userId) {
    String tenantId = requireTenantId();
    if (userId == null || meetingRoomId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    Member member = memberReader.getMember(tenantId, userId);
    if (member == null || member.memberType() != MemberType.HR) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    meetingRoomReader.getMeetingRoom(meetingRoomId);
  }

  @Override
  public void validateReserveRequest(
      Long userId, Long meetingRoomId, LocalDateTime startDatetime, LocalDateTime endDatetime) {
    requireTenantId();
    if (userId == null || meetingRoomId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (startDatetime == null || endDatetime == null || !startDatetime.isBefore(endDatetime)) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
    if (!userReader.exists(userId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    meetingRoomReader.getMeetingRoom(meetingRoomId);
    long conflicts =
        meetingRoomReader.countOverlappingReservations(meetingRoomId, startDatetime, endDatetime);
    if (conflicts > 0) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }
  }

  @Override
  public void validateCancelReservationRequest(
      Long userId, Long meetingRoomId, Long reservationId) {
    requireTenantId();
    if (userId == null || meetingRoomId == null || reservationId == null) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    if (!userReader.exists(userId)) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    meetingRoomReader.getActiveReservation(meetingRoomId, reservationId);
  }

  private String requireTenantId() {
    String tenantId = TenantContext.getTenantId();
    if (tenantId == null || tenantId.isBlank()) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
    return tenantId;
  }
}
