package com.coffiness.calfit.api.v1.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

public record MeetingRoomReservationCreateRequest(
    String title,
    String description,
    @NotNull LocalDateTime startDatetime,
    @NotNull LocalDateTime endDatetime,
    List<Long> participantMemberIds,
    @JsonAlias("participantUserIds") List<Long> participantUserIds) {

  public MeetingRoomReservationCreateRequest(
      String title,
      String description,
      LocalDateTime startDatetime,
      LocalDateTime endDatetime,
      List<Long> participantMemberIds) {
    this(title, description, startDatetime, endDatetime, participantMemberIds, null);
  }
}
