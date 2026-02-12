package com.coffiness.calfit.api.admin.invoice.detail;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/invoices/{id}")
public class GET_specs {

    @Test
    void 존재하지_않는_인보이스_ID_사용_시_404_Not_Found를_반환한다() {
    }

    @Test
    void 인보이스_정보를_올바르게_반환한다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {
    }

}