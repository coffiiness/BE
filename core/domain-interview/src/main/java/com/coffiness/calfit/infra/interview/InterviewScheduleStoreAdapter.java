package com.coffiness.calfit.infra.interview;

import com.coffiness.calfit.core.enums.InterviewStatus;
import com.coffiness.calfit.core.enums.MeetingRoomStatus;
import com.coffiness.calfit.domain.interview.command.InterviewCreateCommand;
import com.coffiness.calfit.domain.interview.command.model.InterviewSchedule;
import com.coffiness.calfit.domain.interview.port.InterviewScheduleStorePort;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleApplicantEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleEntity;
import com.coffiness.calfit.storage.db.core.interview.InterviewScheduleInterviewerEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomReservationEntity;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewScheduleStoreAdapter implements InterviewScheduleStorePort {

  private final EntityManager entityManager;

  @Override
  public InterviewSchedule create(String tenantId, Long userId, InterviewCreateCommand command) {
    InterviewScheduleEntity scheduleEntity =
        new InterviewScheduleEntity(
            command.recruitmentId(),
            command.recruitmentStageId(),
            command.meetingRoomId(),
            command.scheduledAt(),
            command.durationMinutes(),
            command.memo() == null ? "" : command.memo(),
            InterviewStatus.PENDING);

    entityManager.persist(scheduleEntity);
    entityManager.flush();

    Long scheduleId = scheduleEntity.getId();

    for (Long interviewerId : command.interviewerIds()) {
      entityManager.persist(new InterviewScheduleInterviewerEntity(scheduleId, interviewerId));
    }

    for (Long applicantId : command.applicantIds()) {
      entityManager.persist(new InterviewScheduleApplicantEntity(scheduleId, applicantId));
    }

    Long reservationUserId =
        userId != null ? userId : command.interviewerIds().stream().findFirst().orElse(0L);
    MeetingRoomReservationEntity reservation =
        MeetingRoomReservationEntity.builder()
            .meetingRoomId(command.meetingRoomId())
            .userId(reservationUserId)
            .interviewScheduleId(scheduleId)
            .startDatetime(command.scheduledAt())
            .endDatetime(command.scheduledAt().plusMinutes(command.durationMinutes()))
            .status(MeetingRoomStatus.RESERVED)
            .build();
    entityManager.persist(reservation);

    return new InterviewSchedule(
        scheduleId,
        command.recruitmentId(),
        command.recruitmentStageId(),
        command.round(),
        command.meetingRoomId(),
        command.scheduledAt(),
        command.durationMinutes(),
        command.memo(),
        InterviewStatus.PENDING,
        command.interviewerIds(),
        command.applicantIds());
  }
}
