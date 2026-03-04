// package com.coffiness.calfit.api.fixture;
//
// import com.coffiness.calfit.api.v1.request.InterviewAutoAssignRequest;
// import com.coffiness.calfit.api.v1.request.InterviewCreateRequest;
// import com.coffiness.calfit.api.v1.response.InterviewResponse;
// import com.coffiness.calfit.core.enums.InterviewRound;
// import com.coffiness.calfit.core.support.response.ApiResponse;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import java.time.LocalDateTime;
// import java.util.List;
// import org.springframework.core.env.Environment;
// import org.springframework.http.HttpEntity;
// import org.springframework.http.HttpHeaders;
// import org.springframework.http.HttpMethod;
// import org.springframework.http.MediaType;
// import org.springframework.http.ResponseEntity;
//
// public record InterviewFixture(BaseFixture base) {
//
//  public static InterviewFixture create(Environment environment, ObjectMapper objectMapper) {
//    return new InterviewFixture(BaseFixture.create(environment, objectMapper));
//  }
//
//  public ApiResponse<InterviewResponse> create(
//      String token,
//      String tenantId,
//      Long recruitmentId,
//      Long recruitmentStageId,
//      InterviewRound round,
//      List<Long> interviewerIds,
//      List<Long> applicantIds,
//      Long meetingRoomId,
//      LocalDateTime scheduledAt,
//      Integer durationMinutes,
//      String memo) {
//    return exchangeWithTenant(
//        "/api/v1/interviews",
//        HttpMethod.POST,
//        new InterviewCreateRequest(
//            recruitmentId,
//            recruitmentStageId,
//            round,
//            interviewerIds,
//            applicantIds,
//            meetingRoomId,
//            scheduledAt,
//            durationMinutes,
//            memo),
//        token,
//        tenantId,
//        InterviewResponse.class);
//  }
//
//  public ApiResponse<InterviewResponse> autoAssign(
//      String token,
//      String tenantId,
//      Long recruitmentId,
//      Long recruitmentStageId,
//      InterviewRound round,
//      List<Long> interviewerIds,
//      List<Long> applicantIds,
//      Integer durationMinutes,
//      String memo,
//      LocalDateTime searchStart,
//      LocalDateTime searchEnd,
//      Integer slotMinutes,
//      Integer preferredLocation,
//      Integer requiredCapacity) {
//    return exchangeWithTenant(
//        "/api/v1/interviews/auto-assign",
//        HttpMethod.POST,
//        new InterviewAutoAssignRequest(
//            recruitmentId,
//            recruitmentStageId,
//            round,
//            interviewerIds,
//            applicantIds,
//            durationMinutes,
//            memo,
//            searchStart,
//            searchEnd,
//            slotMinutes,
//            preferredLocation,
//            requiredCapacity),
//        token,
//        tenantId,
//        InterviewResponse.class);
//  }
//
//  public ApiResponse<InterviewInvitationResponse> sendInvitation(
//      String token, String tenantId, Long interviewScheduleId, String message) {
//    return exchangeWithTenant(
//        "/api/v1/interviews/" + interviewScheduleId + "/invitations",
//        HttpMethod.POST,
//        new InterviewInvitationSendRequest(message),
//        token,
//        tenantId,
//        InterviewInvitationResponse.class);
//  }
//
//  public ApiResponse<InterviewInvitationAcceptResponse> acceptInvitation(
//      String token, String tenantId, Long interviewScheduleId) {
//    return exchangeWithTenant(
//        "/api/v1/interviews/" + interviewScheduleId + "/invitations/accept",
//        HttpMethod.POST,
//        null,
//        token,
//        tenantId,
//        InterviewInvitationAcceptResponse.class);
//  }
//
//  public ApiResponse<InterviewInvitationDeclineResponse> declineInvitation(
//      String token, String tenantId, Long interviewScheduleId, String reason) {
//    return exchangeWithTenant(
//        "/api/v1/interviews/" + interviewScheduleId + "/invitations/decline",
//        HttpMethod.POST,
//        new InterviewInvitationDeclineRequest(reason),
//        token,
//        tenantId,
//        InterviewInvitationDeclineResponse.class);
//  }
//
//  @SuppressWarnings("unchecked")
//  private <T> ApiResponse<T> exchangeWithTenant(
//      String url,
//      HttpMethod method,
//      Object body,
//      String token,
//      String tenantId,
//      Class<T> responseType) {
//    HttpHeaders headers = new HttpHeaders();
//    headers.setContentType(MediaType.APPLICATION_JSON);
//    if (token != null && !token.isBlank()) {
//      headers.setBearerAuth(token);
//    }
//    if (tenantId != null && !tenantId.isBlank()) {
//      headers.set("X-Tenant-ID", tenantId);
//    }
//    HttpEntity<?> entity =
//        (body != null) ? new HttpEntity<>(body, headers) : new HttpEntity<>(headers);
//
//    ResponseEntity<ApiResponse> response =
//        base.client().exchange(url, method, entity, ApiResponse.class);
//    return convertResponse(response.getBody(), responseType);
//  }
//
//  @SuppressWarnings("unchecked")
//  private <T> ApiResponse<T> convertResponse(ApiResponse<?> response, Class<T> responseType) {
//    if (response == null) {
//      return null;
//    }
//
//    if (response.getResult() == com.coffiness.calfit.core.support.response.ResultType.ERROR
//        || response.getData() == null) {
//      return (ApiResponse<T>) response;
//    }
//
//    if (responseType == Void.class) {
//      return (ApiResponse<T>) response;
//    }
//
//    T convertedData = base.objectMapper().convertValue(response.getData(), responseType);
//    return ApiResponse.success(convertedData);
//  }
// }
