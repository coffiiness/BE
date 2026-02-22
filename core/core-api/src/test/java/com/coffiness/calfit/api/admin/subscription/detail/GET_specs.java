package com.coffiness.calfit.api.admin.subscription.detail;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/subscriptions/{id}")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 존재하지_않는_구독_ID_사용_시_404_Not_Found를_반환한다() {}

  @Test
  void 구독_정보와_결제_이력을_올바르게_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다() {}
}
