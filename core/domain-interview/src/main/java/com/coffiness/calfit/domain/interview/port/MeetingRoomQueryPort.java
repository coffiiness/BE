package com.coffiness.calfit.domain.interview.port;

import com.coffiness.calfit.domain.interview.command.model.MeetingRoomCandidate;
import java.util.List;

// 자동 배정용 회의실 후보 조회 포트
public interface MeetingRoomQueryPort {

  List<MeetingRoomCandidate> findReservableRooms(
      String tenantId, Integer preferredLocation, Integer requiredCapacity);
}
