package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.MeetingRoomCreateRequest;
import com.coffiness.calfit.api.v1.request.MeetingRoomReservationCreateRequest;
import com.coffiness.calfit.api.v1.request.MeetingRoomUpdateRequest;
import com.coffiness.calfit.api.v1.response.MeetingRoomReservationResponse;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomReservation;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/meeting-rooms")
public class MeetingRoomController {

  private final MeetingRoomService meetingRoomService;

  /* 회의실 생성 */
  @PostMapping
  public ApiResponse<MeetingRoomResponse> create(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody MeetingRoomCreateRequest request) {

    Long userId = (user != null) ? user.userId() : null;
    MeetingRoom room =
        meetingRoomService.create(request.name(), request.location(), request.capacity(), userId);

    return ApiResponse.success(MeetingRoomResponse.from(room));
  }

  /* 회의실 조회 */
  @GetMapping
  public ApiResponse<List<MeetingRoom>> list(@AuthenticationPrincipal SecurityUser user) {

    List<MeetingRoom> result = meetingRoomService.list();

    return ApiResponse.success(result);
  }

  /* 회의실 수정 */
  @PutMapping("/{id}")
  public ApiResponse<MeetingRoomResponse> update(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long id,
      @Valid @RequestBody MeetingRoomUpdateRequest request) {

    Long userId = (user != null) ? user.userId() : null;
    MeetingRoom room = meetingRoomService.update(id, request.name(), request.capacity(), userId);

    return ApiResponse.success(MeetingRoomResponse.from(room));
  }

  /* 회의실 삭제 */
  @DeleteMapping("/{id}")
  public ApiResponse<?> delete(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {

    Long userId = (user != null) ? user.userId() : null;
    meetingRoomService.delete(id, userId);

    return ApiResponse.success();
  }

  /* 회의실 예약 */
  @GetMapping("/reservations")
  public ApiResponse<List<MeetingRoomReservationResponse>> listReservations(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDatetime,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDatetime) {

    List<MeetingRoomReservationResponse> result =
        meetingRoomService.listReservations(fromDatetime, toDatetime).stream()
            .map(MeetingRoomReservationResponse::from)
            .toList();

    return ApiResponse.success(result);
  }

  /* 회의실 예약 */
  @PostMapping("/{meetingRoomId}/reservations")
  public ApiResponse<MeetingRoomReservationResponse> reserve(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long meetingRoomId,
      @Valid @RequestBody MeetingRoomReservationCreateRequest request) {

    Long userId = user.userId();
    MeetingRoomReservation reservation =
        meetingRoomService.reserve(
            userId, meetingRoomId, request.startDatetime(), request.endDatetime());

    return ApiResponse.success(MeetingRoomReservationResponse.from(reservation));
  }

  /* 회의실 예약 취소 */
  @DeleteMapping("/{meetingRoomId}/reservations/{reservationId}")
  public ApiResponse<MeetingRoomReservationResponse> cancelReservation(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long meetingRoomId,
      @PathVariable Long reservationId) {

    Long userId = user.userId();
    MeetingRoomReservation canceled =
        meetingRoomService.cancelReservation(userId, meetingRoomId, reservationId);
    return ApiResponse.success(MeetingRoomReservationResponse.from(canceled));
  }
}
