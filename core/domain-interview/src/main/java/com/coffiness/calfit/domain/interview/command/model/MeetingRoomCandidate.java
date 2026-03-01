package com.coffiness.calfit.domain.interview.command.model;

// 자동 배정 시 사용하는 회의실 후보 모델
public record MeetingRoomCandidate(Long id, String name, Integer location, Integer capacity) {}
