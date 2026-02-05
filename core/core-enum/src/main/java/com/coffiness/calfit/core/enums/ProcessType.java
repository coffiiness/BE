package com.coffiness.calfit.core.enums;

public enum ProcessType {

    DOCUMENT("서류 단계"), INTERVIEW("면접 단계"), TEST("시험 단계");

    private final String description;

    ProcessType(String description) {
        this.description = description;
    }

}
