package com.coffiness.calfit.api.member.list;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /api/v1/members")
public class GET_specs {

  @Test
  void 올바른_토큰이면_200_OK와_멤버_목록을_반환한다() {}

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 다른_워크스페이스의_멤버가_포함되어서는_안_된다() {}

  @Test
  void 각_MemberView의_id_email_name_role이_올바르게_반환된다() {}

  @Test
  void 멤버_목록은_최신순으로_정렬된다() {}

  @Test
  void 멤버가_없으면_빈_리스트를_반환한다() {}
}
