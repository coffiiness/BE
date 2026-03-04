package com.coffiness.calfit.domain.recruitment;

/*
 * 면접관 정보
 * */
public record InterviewerInfo(Long memberId, String name, boolean enabled // 체크 상태
    ) {}
