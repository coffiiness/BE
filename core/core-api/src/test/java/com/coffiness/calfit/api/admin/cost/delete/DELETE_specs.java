package com.coffiness.calfit.api.admin.cost.delete;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("DELETE /admin/costs/{id}")
public class DELETE_specs {

    @Test
    void 올바르게_요청하면_204_No_Content를_반환한다() {
    }

    @Test
    void 삭제_후_다시_조회하면_404_Not_Found를_반환한다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {
    }

}
