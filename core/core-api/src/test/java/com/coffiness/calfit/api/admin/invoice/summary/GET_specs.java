package com.coffiness.calfit.api.admin.invoice.summary;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/invoices/summary")
public class GET_specs {

    @Test
    void confirmedRevenue를_올바르게_계산한다() {
    }

    @Test
    void outstanding에_미결제와_연체를_합산한다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {
    }

    @Test
    void month_매개변수가_지정되지_않으면_현재_월_기준으로_반환한다() {
    }

    @Test
    void outstandingOverdue는_연체_건수만_포함한다() {
    }

}
