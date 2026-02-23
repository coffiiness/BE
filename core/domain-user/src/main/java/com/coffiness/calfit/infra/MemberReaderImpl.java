package com.coffiness.calfit.infra;

import com.coffiness.calfit.domain.member.MemberReader;
import com.coffiness.calfit.storage.db.core.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MemberReaderImpl implements MemberReader {
  private final UserRepository userRepository;
}
