package com.coffiness.calfit.api.admin.pnl.summary;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/pnl/summary")
public class GET_specs {

    @Test
    void 영업이익_및_이익률_계산이_정확하다() {
    }

    @Test
    void 전월_대비_증감률_계산이_정확하다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {
    }

    @Test
    void month_매개변수가_지정되지_않으면_현재_월_기준으로_반환한다() {
    }

    @Test
    void operatingProfit은_totalRevenue에서_totalCost를_뺀_값이다() {
    }

    @Test
    void operatingMargin은_소수점_1자리까지_표시한다() {
    }

}
