package com.coffiness.calfit.api.member.update;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("PATCH /api/v1/member/{memberId}")
public class PATCH_specs {

  @Test
  void 정상_요청_시_204를_반환한다() {}

  @Test
  void 유효하지_않은_role이면_400_Bad_Request를_반환한다() {}

  @Test
  void 존재하지_않는_groupId_지정_시_에러를_반환한다() {}

  @Test
  void 권한_없는_사용자가_요청하면_403_Forbidden을_반환한다() {}

  @Test
  void 존재하지_않는_memberId이면_404_Not_Found를_반환한다() {}

  @Test
  void role만_변경해도_정상_처리된다() {}

  @Test
  void groupId만_변경해도_정상_처리된다() {}

  @Test
  void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 다른_워크스페이스의_멤버를_수정하면_에러를_반환한다() {}

  @Test
  void 요청_본문이_비어있으면_400_Bad_Request를_반환한다() {}
}
