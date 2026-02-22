package com.coffiness.calfit.api.admin.cost.trend;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/costs/trend")
public class GET_specs {

  @Test
  void 최근_6개월_데이터를_반환한다() {}

  @Test
  void 시간_순서로_정렬한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}
}
