package com.coffiness.calfit.api.admin.cost.create;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /admin/costs")
public class POST_specs {

    @Test
    void 올바르게_요청하면_201_Created와_Location_헤더를_반환한다() {
    }

    @Test
    void 필수_속성_누락_시_400_Bad_Request를_반환한다() {
    }

    @Test
    void category가_허용된_값이_아니면_400_Bad_Request를_반환한다() {
    }

    @Test
    void amount가_0_이하이면_400_Bad_Request를_반환한다() {
    }

    @Test
    void month_형식이_올바르지_않으면_400_Bad_Request를_반환한다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {
    }

}
