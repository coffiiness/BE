package com.coffiness.calfit.domain;

import java.util.List;

public interface ScheduleStore {

  Schedule store(Schedule schedule, List<Long> attendeeIds);

  void update(Schedule schedule, List<Long> attendeeIds);

  void delete(Schedule schedule);
}
