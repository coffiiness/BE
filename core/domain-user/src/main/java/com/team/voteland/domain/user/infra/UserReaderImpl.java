package com.team.voteland.domain.user.infra;

import com.team.voteland.domain.user.domain.UserInfo;
import com.team.voteland.domain.user.domain.UserReader;
import com.team.voteland.storage.db.core.user.UserEntity;
import com.team.voteland.storage.db.core.user.UserRepository;

import org.springframework.stereotype.Component;

/**
 * UserReader 구현체 - 다른 도메인에서 User 정보 조회시 사용
 */
@Component
public class UserReaderImpl implements UserReader {

    private final UserRepository userRepository;

    public UserReaderImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserInfo getUser(Long userId) {
        return userRepository.findById(userId).map(this::toUserInfo).orElse(null);
    }

    @Override
    public boolean exists(Long userId) {
        return userRepository.existsById(userId);
    }

    private UserInfo toUserInfo(UserEntity entity) {
        return new UserInfo(entity.getId(), entity.getEmail(), entity.getName(), entity.getRole().name());
    }

}
