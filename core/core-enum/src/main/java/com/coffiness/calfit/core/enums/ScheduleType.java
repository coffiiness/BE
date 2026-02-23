package com.coffiness.calfit.core.enums;

public enum ScheduleType {
  MEETING("회의"),
  VACATION("휴가"),
  BUSINESS("외근/출장"),
  OTHERS("기타");

  private String description;

  private ScheduleType(final String description) {
    this.description = description;
  }
}
