package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.interview.InterviewAvailability;
import com.coffiness.calfit.domain.interview.InterviewReader;
import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
import com.coffiness.calfit.domain.interview.WeeklyInterviewScheduleItem;
import com.coffiness.calfit.storage.db.core.interview.InterviewRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewReaderImpl implements InterviewReader {

  private final InterviewRepository interviewRepository;

  @Override
  public boolean isHrMember(Long userId) {
    return interviewRepository.isHrMember(userId);
  }

  @Override
  public int getMeetingRoomCapacity(Long meetingRoomId) {
    return interviewRepository.getMeetingRoomCapacity(meetingRoomId);
  }

  @Override
  public List<InterviewAvailability.MeetingRoomBusySlot> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to) {
    return interviewRepository.findMeetingRoomBusySlots(meetingRoomIds, from, to).stream()
        .map(
            row ->
                new InterviewAvailability.MeetingRoomBusySlot(
                    row.meetingRoomId(), row.interviewScheduleId(), row.start(), row.end()))
        .toList();
  }

  @Override
  public List<InterviewAvailability.InterviewerBusySlot> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to) {
    return interviewRepository.findInterviewerBusySlots(interviewerIds, from, to).stream()
        .map(
            row ->
                new InterviewAvailability.InterviewerBusySlot(
                    row.interviewerId(), row.interviewScheduleId(), row.start(), row.end()))
        .toList();
  }

  @Override
  public List<InterviewAvailability.ApplicantBusySlot> findApplicantBusySlots(
      List<Long> applicantIds, LocalDateTime from, LocalDateTime to) {
    return interviewRepository.findApplicantBusySlots(applicantIds, from, to).stream()
        .map(
            row ->
                new InterviewAvailability.ApplicantBusySlot(
                    row.applicantId(), row.interviewScheduleId(), row.start(), row.end()))
        .toList();
  }

  @Override
  public List<InterviewScheduleCalendarItem> getSchedulesByRecruitmentId(
      Long recruitmentId, LocalDateTime from, LocalDateTime to) {
    return interviewRepository.getSchedulesByRecruitmentId(recruitmentId, from, to).stream()
        .map(
            row ->
                new InterviewScheduleCalendarItem(
                    row.interviewScheduleId(),
                    row.startAt(),
                    row.endAt(),
                    row.interviewerUserId(),
                    row.interviewerName(),
                    row.title(),
                    row.applicantName(),
                    row.description()))
        .toList();
  }

  // 저장소 조회 결과를 주간 면접 일정 전용 도메인 모델로 변환
  @Override
  public List<WeeklyInterviewScheduleItem> getWeeklySchedules(
      Long interviewerUserId, LocalDateTime from, LocalDateTime to) {
    return interviewRepository.getWeeklySchedules(interviewerUserId, from, to).stream()
        .map(
            row ->
                new WeeklyInterviewScheduleItem(
                    row.interviewScheduleId(),
                    row.recruitmentId(),
                    row.startAt(),
                    row.endAt(),
                    row.interviewerUserId(),
                    row.interviewerName(),
                    row.title(),
                    row.applicantName(),
                    row.description(),
                    row.location()))
        .toList();
  }
}
