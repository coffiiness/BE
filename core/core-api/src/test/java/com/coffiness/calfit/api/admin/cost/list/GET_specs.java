package com.coffiness.calfit.api.admin.cost.list;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/costs")
public class GET_specs {

  @Test
  void 카테고리_및_월_필터가_올바르게_동작한다() {}

  @Test
  void 페이지네이션이_올바르게_동작한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 필터_없이_요청하면_전체_목록을_반환한다() {}
}
