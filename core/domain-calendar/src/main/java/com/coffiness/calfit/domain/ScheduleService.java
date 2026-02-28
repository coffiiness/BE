package com.coffiness.calfit.domain;

import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;

    public void createSchedule(Long userId, ScheduleCreateRequest request) {
        ScheduleEntity scheduleEntity = ScheduleEntity.builder()
                .userId(userId)
                .title(request.title())
                .description(request.description())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .isAllDay(request.isAllDay() != null ? request.isAllDay() : false)
                .roomId(request.roomId())
                .type(request.type())
                .isBusy(request.isBusy() != null ? request.isBusy() : true)
                .build();

        scheduleRepository.save(scheduleEntity);
    }
}
