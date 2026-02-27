package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.AnnouncementBoardCreateRequest;
import com.coffiness.calfit.api.v1.request.AnnouncementBoardUpdateRequest;
import com.coffiness.calfit.api.v1.response.AnnouncementBoardResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoard;
import com.coffiness.calfit.domain.announcementBoard.AnnouncementBoardService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/announcement-boards")
public class AnnouncementBoardController {

  private final AnnouncementBoardService announcementBoardService;

  /* 공지사항 생성 */
  @PostMapping
  public ApiResponse<AnnouncementBoardResponse> create(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody AnnouncementBoardCreateRequest request) {
    AnnouncementBoard board =
        announcementBoardService.create(request.title(), request.content(), request.pinned());
    return ApiResponse.success(AnnouncementBoardResponse.from(board));
  }

  /* 공지사항 조회 */
  @GetMapping
  public ApiResponse<List<AnnouncementBoard>> list(@AuthenticationPrincipal SecurityUser user) {
    List<AnnouncementBoard> result = announcementBoardService.list();
    return ApiResponse.success(result);
  }

  /* 공지사항  수정 */
  @PutMapping("/{id}")
  public ApiResponse<AnnouncementBoardResponse> update(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long id,
      @Valid @RequestBody AnnouncementBoardUpdateRequest request) {
    AnnouncementBoard board =
        announcementBoardService.update(id, request.title(), request.content(), request.pinned());
    return ApiResponse.success(AnnouncementBoardResponse.from(board));
  }

  /* 공지사항 삭제 */
  @DeleteMapping("/{id}")
  public ApiResponse<?> delete(@AuthenticationPrincipal SecurityUser user, @PathVariable Long id) {
    Long userId = (user != null) ? user.userId() : null;
    announcementBoardService.delete(id, userId);
    return ApiResponse.success();
  }
}
