package com.coffiness.calfit.api.admin.dashboard.summary;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/dashboard/summary")
public class GET_specs {

  @Test
  void 올바르게_요청하면_200_OK_상태코드를_반환한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 관리자가_아닌_사용자의_접근_토큰을_사용하면_403_Forbidden을_반환한다() {}

  @Test
  void MRR_구독_고객_수_이탈률_월간_비용_합계를_올바르게_반환한다() {}

  @Test
  void month_매개변수가_지정되지_않으면_현재_월_기준으로_반환한다() {}

  @Test
  void mrrGrowth는_전월_대비_증감률을_올바르게_계산한다() {}

  @Test
  void 잘못된_month_형식이면_400_Bad_Request를_반환한다() {}

  @Test
  void 데이터가_없는_월을_조회하면_모든_값이_0으로_반환된다() {}
}
