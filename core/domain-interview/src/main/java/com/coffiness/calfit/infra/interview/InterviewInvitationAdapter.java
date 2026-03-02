package com.coffiness.calfit.infra.interview;

import com.coffiness.calfit.core.enums.InterviewResponseStatus;
import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationAcceptResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationDeclineResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitationResult;
import com.coffiness.calfit.domain.interview.command.model.InterviewInvitee;
import com.coffiness.calfit.domain.interview.port.InterviewInvitationPort;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantEntity;
import com.coffiness.calfit.storage.db.core.applicant.ApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewResponseEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewResponseRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerRepository;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleRepository;
import com.coffiness.calfit.storage.db.core.user.UserEntity;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewInvitationAdapter implements InterviewInvitationPort {

  private final InterviewScheduleRepository interviewScheduleRepository;
  private final InterviewScheduleInterviewerRepository interviewerRepository;
  private final InterviewScheduleApplicantRepository applicantMappingRepository;
  private final InterviewResponseRepository interviewResponseRepository;
  private final UserRepository userRepository;
  private final ApplicantRepository applicantRepository;

  @Override
  @Transactional
  public InterviewInvitationResult sendInvitations(
      String tenantId, Long userId, Long interviewScheduleId, String message) {
    InterviewScheduleEntity schedule = getScheduleOrThrow(tenantId, interviewScheduleId);

    List<InterviewInvitee> invitees = loadInvitees(tenantId, interviewScheduleId);

    if (invitees.isEmpty()) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }

    // 초대 발송(재발송 포함): 기존 응답 초기화 후 PENDING 유지
    interviewResponseRepository.deleteByTenantIdAndInterviewScheduleId(
        tenantId, interviewScheduleId);
    schedule.pending();

    LocalDateTime sentAt = LocalDateTime.now();
    for (InterviewInvitee invitee : invitees) {
      // 실제 메일/SMS 연동 전까지는 발송 로그로 대체
      log.info(
          "Interview invitation sent tenantId={}, interviewScheduleId={}, sentBy={}, targetType={}, targetId={}, email={}, message={}",
          tenantId,
          interviewScheduleId,
          userId,
          invitee.targetType(),
          invitee.targetId(),
          invitee.email(),
          message);
    }

    return new InterviewInvitationResult(interviewScheduleId, message, sentAt, invitees);
  }

  @Override
  @Transactional
  public InterviewInvitationAcceptResult acceptInvitation(
      String tenantId, Long interviewerUserId, Long interviewScheduleId) {
    InterviewScheduleEntity schedule = getScheduleOrThrow(tenantId, interviewScheduleId);
    assertInterviewerAssigned(tenantId, interviewScheduleId, interviewerUserId);

    LocalDateTime now = LocalDateTime.now();
    InterviewResponseEntity response =
        interviewResponseRepository
            .findByTenantIdAndInterviewScheduleIdAndUserId(
                tenantId, interviewScheduleId, interviewerUserId)
            .orElseGet(
                () -> InterviewResponseEntity.accepted(interviewScheduleId, interviewerUserId));
    response.accept();
    interviewResponseRepository.save(response);

    int totalInterviewerCount = countAssignedInterviewers(tenantId, interviewScheduleId);
    int acceptedCount = countAcceptedInterviewers(tenantId, interviewScheduleId);
    boolean finalized = totalInterviewerCount > 0 && totalInterviewerCount == acceptedCount;

    // 면접관 전원 수락 시 최종 확정
    InterviewStatus status = finalized ? InterviewStatus.CONFIRMED : InterviewStatus.PENDING;
    if (finalized) {
      schedule.confirm();
    }

    return new InterviewInvitationAcceptResult(
        interviewScheduleId,
        interviewerUserId,
        now,
        acceptedCount,
        totalInterviewerCount,
        finalized,
        status);
  }

  @Override
  @Transactional
  public InterviewInvitationDeclineResult declineInvitation(
      String tenantId, Long interviewerUserId, Long interviewScheduleId, String declineReason) {
    InterviewScheduleEntity schedule = getScheduleOrThrow(tenantId, interviewScheduleId);
    assertInterviewerAssigned(tenantId, interviewScheduleId, interviewerUserId);
    if (declineReason == null || declineReason.isBlank()) {
      throw new CoreException(ErrorType.VALIDATION_ERROR);
    }

    LocalDateTime now = LocalDateTime.now();
    InterviewResponseEntity response =
        interviewResponseRepository
            .findByTenantIdAndInterviewScheduleIdAndUserId(
                tenantId, interviewScheduleId, interviewerUserId)
            .orElseGet(
                () -> InterviewResponseEntity.accepted(interviewScheduleId, interviewerUserId));
    response.decline(declineReason);
    interviewResponseRepository.save(response);

    // 한 명이라도 거절하면 재조율 상태로 유지
    schedule.pending();

    int totalInterviewerCount = countAssignedInterviewers(tenantId, interviewScheduleId);
    int acceptedCount = countAcceptedInterviewers(tenantId, interviewScheduleId);
    int declinedCount =
        interviewResponseRepository.countByTenantIdAndInterviewScheduleIdAndResponseStatus(
            tenantId, interviewScheduleId, InterviewResponseStatus.DECLINED);

    return new InterviewInvitationDeclineResult(
        interviewScheduleId,
        interviewerUserId,
        declineReason,
        now,
        acceptedCount,
        declinedCount,
        totalInterviewerCount,
        InterviewStatus.PENDING);
  }

  private InterviewScheduleEntity getScheduleOrThrow(String tenantId, Long interviewScheduleId) {
    InterviewScheduleEntity schedule =
        interviewScheduleRepository
            .findByIdAndTenantId(interviewScheduleId, tenantId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    if (schedule.getStatus() == InterviewStatus.CANCELLED) {
      throw new CoreException(ErrorType.NOT_FOUND);
    }
    return schedule;
  }

  private void assertInterviewerAssigned(
      String tenantId, Long interviewScheduleId, Long interviewerUserId) {
    boolean assigned =
        interviewerRepository.existsByTenantIdAndInterviewScheduleIdAndUserId(
            tenantId, interviewScheduleId, interviewerUserId);
    if (!assigned) {
      throw new CoreException(ErrorType.UNAUTHORIZED);
    }
  }

  private int countAssignedInterviewers(String tenantId, Long interviewScheduleId) {
    return interviewerRepository.countByTenantIdAndInterviewScheduleId(
        tenantId, interviewScheduleId);
  }

  private int countAcceptedInterviewers(String tenantId, Long interviewScheduleId) {
    return interviewResponseRepository.countByTenantIdAndInterviewScheduleIdAndResponseStatus(
        tenantId, interviewScheduleId, InterviewResponseStatus.ACCEPTED);
  }

  private List<InterviewInvitee> loadInvitees(String tenantId, Long interviewScheduleId) {
    List<InterviewInvitee> result = new ArrayList<>();

    List<InterviewScheduleInterviewerEntity> interviewerMappings =
        interviewerRepository.findAllByTenantIdAndInterviewScheduleId(
            tenantId, interviewScheduleId);
    List<Long> interviewerIds =
        interviewerMappings.stream().map(InterviewScheduleInterviewerEntity::getUserId).toList();
    Map<Long, UserEntity> userById = new HashMap<>();
    for (UserEntity user : userRepository.findAllById(interviewerIds)) {
      userById.put(user.getId(), user);
    }
    for (Long interviewerId : interviewerIds) {
      UserEntity user = userById.get(interviewerId);
      result.add(
          new InterviewInvitee(
              "INTERVIEWER",
              interviewerId,
              user != null ? user.getName() : null,
              user != null ? user.getEmail() : null));
    }

    List<InterviewScheduleApplicantEntity> applicantMappings =
        applicantMappingRepository.findAllByTenantIdAndInterviewScheduleId(
            tenantId, interviewScheduleId);
    List<Long> applicantIds =
        applicantMappings.stream().map(InterviewScheduleApplicantEntity::getApplicantId).toList();
    Map<Long, ApplicantEntity> applicantById = new HashMap<>();
    for (ApplicantEntity applicant :
        applicantRepository.findAllByTenantIdAndIdIn(tenantId, applicantIds)) {
      applicantById.put(applicant.getId(), applicant);
    }
    for (Long applicantId : applicantIds) {
      ApplicantEntity applicant = applicantById.get(applicantId);
      result.add(
          new InterviewInvitee(
              "APPLICANT",
              applicantId,
              applicant != null ? applicant.getName() : null,
              applicant != null ? applicant.getEmail() : null));
    }

    return result;
  }
}
