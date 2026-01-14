package com.team.voteland.domain.user.domain;

/**
 * 외부 도메인에 제공하는 User 정보 DTO
 */
public record UserInfo(Long id, String email, String name, String role) {
}
