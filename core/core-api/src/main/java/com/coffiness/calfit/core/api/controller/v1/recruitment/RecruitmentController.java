package com.coffiness.calfit.core.api.controller.v1.recruitment;

import com.coffiness.calfit.api.v1.request.RecruitmentCreateRequest;
import com.coffiness.calfit.api.v1.response.InterviewScheduleResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentDetailResponse;
import com.coffiness.calfit.api.v1.response.RecruitmentListResponse;
import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.domain.interview.InterviewScheduleCalendarItem;
import com.coffiness.calfit.domain.recruitment.RecruitmentDetailInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentListInfo;
import com.coffiness.calfit.domain.recruitment.RecruitmentService;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Validated
public class RecruitmentController {

  private final RecruitmentService recruitmentService;

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/api/v1/recruitments")
  public ApiResponse<Long> createRecruitment(
      @AuthenticationPrincipal SecurityUser user,
      @Valid @RequestBody RecruitmentCreateRequest request) {

    long userId = user.userId();

    Long RecruitmentId = recruitmentService.createRecruitment(userId, request);

    return ApiResponse.success(RecruitmentId);
  }

  @GetMapping("/api/v1/recruitments")
  public ApiResponse<List<RecruitmentListResponse>> getRecruitmentList(
      @AuthenticationPrincipal SecurityUser user,
      @RequestParam(required = false) RecruitmentStatus recruitmentStatus,
      @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC)
          Pageable pageable) {
    long userId = user.userId();

    List<RecruitmentListInfo> infos =
        recruitmentService.getRecruitmentList(userId, recruitmentStatus, pageable);

    List<RecruitmentListResponse> response =
        infos.stream().map(RecruitmentListResponse::from).toList();

    return ApiResponse.success(response);
  }

  @GetMapping("/api/v1/recruitments/{recruitmentId}")
  public ApiResponse<RecruitmentDetailResponse> getRecruitmentDetail(
      @AuthenticationPrincipal SecurityUser user, @PathVariable Long recruitmentId) {
    long userId = user.userId();

    RecruitmentDetailInfo info = recruitmentService.getRecruitmentDetail(userId, recruitmentId);

    return ApiResponse.success(RecruitmentDetailResponse.from(info));
  }

  @GetMapping("/api/v1/recruitments/{recruitmentId}/interview-schedules")
  public ApiResponse<List<InterviewScheduleResponse>> getInterviewSchedule(
      @AuthenticationPrincipal SecurityUser user,
      @PathVariable Long recruitmentId,
      @RequestParam String yearMonth) {
    long userId = user.userId();

    List<InterviewScheduleCalendarItem> items =
        recruitmentService.getInterviewSchedules(userId, recruitmentId, yearMonth);

    List<InterviewScheduleResponse> response =
        items.stream().map(InterviewScheduleResponse::from).toList();

    return ApiResponse.success(response);
  }
}
