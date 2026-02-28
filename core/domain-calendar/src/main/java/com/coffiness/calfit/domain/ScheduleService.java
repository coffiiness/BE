package com.coffiness.calfit.domain;

import com.coffiness.calfit.request.ScheduleCreateRequest;
import com.coffiness.calfit.response.ScheduleResponse;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleEntity;
import com.coffiness.calfit.storage.db.core.calendar.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ScheduleService {

    // TODO : 유저 관련 보안 예외 처리 삽입

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

    public List<ScheduleResponse> getSchedules(long userId, LocalDateTime startDate, LocalDateTime endDate) {

        List<ScheduleEntity> schedules = scheduleRepository.findOverlappingSchedules(userId, startDate, endDate);

        return schedules.stream()
                .map(ScheduleResponse::from)
                .toList();
    }
}
