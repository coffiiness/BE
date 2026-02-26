package com.coffiness.calfit.domain.meetingRoom;

import com.coffiness.calfit.core.enums.MemberType;
import com.coffiness.calfit.domain.workspace.member.Member;
import com.coffiness.calfit.domain.workspace.member.MemberReader;
import com.coffiness.calfit.storage.db.core.config.TenantContext;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomEntity;
import com.coffiness.calfit.storage.db.core.meetingRoom.MeetingRoomRepository;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MeetingRoomService {

    private final MeetingRoomRepository meetingRoomRepository;
    private final MemberReader memberReader;

    /* 회의실 생성 */
    @Transactional
    public MeetingRoom create(String name, Integer location, Integer capacity) {

        if (meetingRoomRepository.existsByName(name)) {
            throw new CoreException(ErrorType.VALIDATION_ERROR);
        }

        MeetingRoomEntity entity =
                MeetingRoomEntity.builder()
                        .name(name)
                        .location(location)
                        .capacity(capacity)
                        .build();

        try {
            MeetingRoomEntity saved =
                    meetingRoomRepository.save(entity);

            return toMeetingRoom(saved);

        } catch (DataIntegrityViolationException e) {
            throw new CoreException(ErrorType.VALIDATION_ERROR);
        }
    }


    /* 회의실 수정 */
    @Transactional
    public MeetingRoom update(Long id, String name, Integer capacity) {

        MeetingRoomEntity entity = meetingRoomRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new CoreException(ErrorType.NOT_FOUND)
                        );

        Optional<MeetingRoomEntity> duplicated =
                meetingRoomRepository
                        .findByName(name);

        if (duplicated.isPresent()
                && !duplicated.get().getId().equals(id)) {

            throw new CoreException(ErrorType.VALIDATION_ERROR);
        }

        entity.update(name, capacity);

        return toMeetingRoom(entity);
    }


    /* 회의실 삭제 */
    @Transactional
    public void delete(Long id, Long userId) {
        String tenantId = TenantContext.getTenantId();
        assertHrMember(tenantId, userId);

        MeetingRoomEntity entity =
                meetingRoomRepository
                        .findById(id)
                        .orElseThrow(() ->
                                new CoreException(ErrorType.NOT_FOUND)
                        );

        meetingRoomRepository.delete(entity);
    }


    /* 회의실 리스트 조회 */
    @Transactional(readOnly = true)
    public List<MeetingRoom> list() {

        List<MeetingRoomEntity> entities =
                meetingRoomRepository
                        .findAllByOrderByNameAsc();

        List<MeetingRoom> result = new ArrayList<>();

        for (MeetingRoomEntity entity : entities) {
            result.add(toMeetingRoom(entity));
        }

        return result;
    }


    /*  MAPPER  */
    private MeetingRoom toMeetingRoom(MeetingRoomEntity entity) {

        return new MeetingRoom(
                entity.getId(),
                entity.getName(),
                entity.getCapacity()
        );
    }

    private void assertHrMember(String tenantId, Long userId) {
        if (tenantId == null || userId == null) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }

        Member member = memberReader.getMember(tenantId, userId);
        if (member == null || member.memberType() != MemberType.HR) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
    }
}
