package com.team.voteland.domain.user.domain;

/**
 * 외부 도메인에서 User 정보를 조회하기 위한 인터페이스 (의존성 역전)
 */
public interface UserReader {

    /**
     * 사용자 ID로 사용자 정보 조회
     * @param userId 사용자 ID
     * @return 사용자 정보 (없으면 null)
     */
    UserInfo getUser(Long userId);

    /**
     * 사용자 존재 여부 확인
     * @param userId 사용자 ID
     * @return 존재 여부
     */
    boolean exists(Long userId);

}
