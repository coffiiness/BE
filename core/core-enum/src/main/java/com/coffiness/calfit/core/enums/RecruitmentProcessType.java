package com.coffiness.calfit.core.enums;

public enum RecruitmentProcessType {
  DOCUMENT("서류 단계"),
  INTERVIEW("면접 단계"),
  TEST("시험 단계");

  private final String description;

  RecruitmentProcessType(String description) {
    this.description = description;
  }
}
