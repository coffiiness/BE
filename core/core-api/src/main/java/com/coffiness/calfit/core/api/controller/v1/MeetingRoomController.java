package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.MeetingRoomCreateRequest;
import com.coffiness.calfit.api.v1.request.MeetingRoomUpdateRequest;
import com.coffiness.calfit.api.v1.response.MeetingRoomResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoom;
import com.coffiness.calfit.domain.meetingRoom.MeetingRoomService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
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

    MeetingRoom room =
        meetingRoomService.create(request.name(), request.location(), request.capacity());

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

    MeetingRoom room = meetingRoomService.update(id, request.name(), request.capacity());

    return ApiResponse.success(MeetingRoomResponse.from(room));
  }

  /* 회의실 삭제 */
  @DeleteMapping("/{id}")
  public ApiResponse<?> delete(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {

    Long userId = (user != null) ? user.userId() : null;
    meetingRoomService.delete(id, userId);

    return ApiResponse.success();
  }
}
