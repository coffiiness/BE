package com.coffiness.calfit.domain.member;

import com.coffiness.calfit.infra.MemberReaderImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {
  private final MemberReaderImpl memberReader;

  public void getMembers() {}
}
