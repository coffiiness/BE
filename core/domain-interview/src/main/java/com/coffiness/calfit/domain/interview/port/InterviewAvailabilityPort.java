package com.coffiness.calfit.domain.interview.port;

import com.coffiness.calfit.domain.interview.command.model.TimeWindow;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// 면접관/지원자/회의실 가용 시간 조회 포트
public interface InterviewAvailabilityPort {

  List<TimeWindow> getInterviewerBusyWindows(
      String tenantId, List<Long> interviewerIds, LocalDateTime from, LocalDateTime to);

  List<TimeWindow> getApplicantBusyWindows(
      String tenantId, List<Long> applicantIds, LocalDateTime from, LocalDateTime to);

  Map<Long, List<TimeWindow>> getMeetingRoomBusyWindows(
      String tenantId, List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to);
}
