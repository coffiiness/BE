package com.coffiness.calfit.infra;

import com.coffiness.calfit.storage.db.core.interview.InterviewRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class InterviewRepositoryImpl implements InterviewRepository {

  /*
   * NOTE:
   * 기존 구현은 InterviewSchedule*Repository 타입들에 의존했는데,
   * 현재 코드베이스에 해당 저장소 인터페이스가 없어 컴파일 에러가 발생합니다.
   * 에러 제거를 위해 구현부를 임시 비활성화(주석 처리 대체)하고 스텁으로 유지합니다.
   */

  @Override
  public boolean isHrMember(Long userId) {
    return false;
  }

  @Override
  public int getMeetingRoomCapacity(Long meetingRoomId) {
    return 0;
  }

  @Override
  public List<MeetingRoomBusySlotRow> findMeetingRoomBusySlots(
      List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to) {
    return List.of();
  }

  @Override
  public List<InterviewerBusySlotRow> findInterviewerBusySlots(
      List<Long> interviewerIds, LocalDateTime from, LocalDateTime to) {
    return List.of();
  }

  @Override
  public Long createConfirmedSchedule(
      Long userId,
      Long recruitmentId,
      Long recruitmentStageId,
      Long meetingRoomId,
      LocalDateTime scheduledAt,
      Integer durationMinutes,
      String memo,
      List<Long> interviewerIds,
      List<Long> applicantIds) {
    throw new UnsupportedOperationException("InterviewRepositoryImpl is temporarily disabled.");
  }
}
