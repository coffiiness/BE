package com.coffiness.calfit.api.admin.cost.summary;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/costs/summary")
public class GET_specs {

  @Test
  void 카테고리를_모두_반환한다() {}

  @Test
  void 각_카테고리의_금액_합계와_percent_합이_100인지_올바르게_계산한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void month_매개변수가_지정되지_않으면_현재_월_기준으로_반환한다() {}

  @Test
  void percent는_소수점_1자리까지_표시한다() {}
}
