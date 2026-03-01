package com.coffiness.calfit.infra.interview;

import com.coffiness.calfit.domain.interview.command.model.TimeWindow;
import com.coffiness.calfit.domain.interview.port.InterviewAvailabilityPort;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InterviewAvailabilityAdapter implements InterviewAvailabilityPort {

  private final EntityManager entityManager;

  @Override
  public List<TimeWindow> getInterviewerBusyWindows(
      String tenantId, List<Long> interviewerIds, LocalDateTime from, LocalDateTime to) {
    if (interviewerIds == null || interviewerIds.isEmpty()) {
      return List.of();
    }

    List<TimeWindow> result = new ArrayList<>();

    Query scheduleQuery =
        entityManager.createNativeQuery(
            "SELECT s.start_time, s.end_time "
                + "FROM schedule s "
                + "WHERE s.tenant_id = :tenantId "
                + "AND s.user_id IN (:userIds) "
                + "AND s.start_time < :toTime "
                + "AND s.end_time > :fromTime");
    scheduleQuery.setParameter("tenantId", tenantId);
    scheduleQuery.setParameter("userIds", interviewerIds);
    scheduleQuery.setParameter("fromTime", from);
    scheduleQuery.setParameter("toTime", to);

    List<Object[]> scheduleRows = scheduleQuery.getResultList();
    for (Object[] row : scheduleRows) {
      LocalDateTime start = toLocalDateTime(row[0]);
      LocalDateTime end = toLocalDateTime(row[1]);
      result.add(new TimeWindow(start, end));
    }

    Query interviewQuery =
        entityManager.createNativeQuery(
            "SELECT i.scheduled_at, i.durationMinutes "
                + "FROM interview_schedules i "
                + "JOIN interview_schedule_interviewers m "
                + "ON m.interview_schedule_id = i.id AND m.tenant_id = i.tenant_id "
                + "WHERE i.tenant_id = :tenantId "
                + "AND m.user_id IN (:userIds) "
                + "AND i.status <> 'CANCELLED' "
                + "AND i.scheduled_at < :toTime");
    interviewQuery.setParameter("tenantId", tenantId);
    interviewQuery.setParameter("userIds", interviewerIds);
    interviewQuery.setParameter("toTime", to);

    List<Object[]> interviewRows = interviewQuery.getResultList();
    appendInterviewRows(result, interviewRows, from);
    return result;
  }

  @Override
  public List<TimeWindow> getApplicantBusyWindows(
      String tenantId, List<Long> applicantIds, LocalDateTime from, LocalDateTime to) {
    if (applicantIds == null || applicantIds.isEmpty()) {
      return List.of();
    }

    List<TimeWindow> result = new ArrayList<>();

    Query interviewQuery =
        entityManager.createNativeQuery(
            "SELECT i.scheduled_at, i.durationMinutes "
                + "FROM interview_schedules i "
                + "JOIN interview_schedule_applicants a "
                + "ON a.interview_schedule_id = i.id AND a.tenant_id = i.tenant_id "
                + "WHERE i.tenant_id = :tenantId "
                + "AND a.applicant_id IN (:applicantIds) "
                + "AND i.status <> 'CANCELLED' "
                + "AND i.scheduled_at < :toTime");
    interviewQuery.setParameter("tenantId", tenantId);
    interviewQuery.setParameter("applicantIds", applicantIds);
    interviewQuery.setParameter("toTime", to);

    List<Object[]> interviewRows = interviewQuery.getResultList();
    appendInterviewRows(result, interviewRows, from);
    return result;
  }

  @Override
  public Map<Long, List<TimeWindow>> getMeetingRoomBusyWindows(
      String tenantId, List<Long> meetingRoomIds, LocalDateTime from, LocalDateTime to) {
    if (meetingRoomIds == null || meetingRoomIds.isEmpty()) {
      return Map.of();
    }

    Query reservationQuery =
        entityManager.createNativeQuery(
            "SELECT r.meeting_room_id, r.start_datetime, r.end_datetime "
                + "FROM meeting_room_reservations r "
                + "WHERE r.tenant_id = :tenantId "
                + "AND r.meeting_room_id IN (:roomIds) "
                + "AND r.status = 'RESERVED' "
                + "AND r.start_datetime < :toTime "
                + "AND r.end_datetime > :fromTime");
    reservationQuery.setParameter("tenantId", tenantId);
    reservationQuery.setParameter("roomIds", meetingRoomIds);
    reservationQuery.setParameter("fromTime", from);
    reservationQuery.setParameter("toTime", to);

    List<Object[]> rows = reservationQuery.getResultList();
    Map<Long, List<TimeWindow>> result = new HashMap<>();
    for (Object[] row : rows) {
      Long roomId = ((Number) row[0]).longValue();
      LocalDateTime start = toLocalDateTime(row[1]);
      LocalDateTime end = toLocalDateTime(row[2]);
      result.computeIfAbsent(roomId, key -> new ArrayList<>()).add(new TimeWindow(start, end));
    }

    return result;
  }

  private void appendInterviewRows(
      List<TimeWindow> target, List<Object[]> rows, LocalDateTime fromLocal) {
    for (Object[] row : rows) {
      LocalDateTime start = toLocalDateTime(row[0]);
      int duration = ((Number) row[1]).intValue();
      LocalDateTime end = start.plusMinutes(duration);
      if (end.isAfter(fromLocal)) {
        target.add(new TimeWindow(start, end));
      }
    }
  }

  private LocalDateTime toLocalDateTime(Object value) {
    if (value instanceof LocalDateTime ldt) {
      return ldt;
    }
    if (value instanceof Timestamp ts) {
      return ts.toLocalDateTime();
    }
    return LocalDateTime.parse(value.toString().replace(' ', 'T'));
  }
}
