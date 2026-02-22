package com.coffiness.calfit.api.admin.dashboard.revenueCostTrend;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/dashboard/revenue-cost-trend")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void period가_monthly이면_최근_8개월_데이터를_반환한다() {}

  @Test
  void period가_quarterly이면_최근_4분기_데이터를_반환한다() {}

  @Test
  void period_매개변수가_지정되지_않으면_월별_기본값을_사용한다() {}

  @Test
  void 매출과_비용은_원_단위_정수로_반환된다() {}

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다() {}

  @Test
  void 유효하지_않은_period_값이면_400_Bad_Request를_반환한다() {}
}
