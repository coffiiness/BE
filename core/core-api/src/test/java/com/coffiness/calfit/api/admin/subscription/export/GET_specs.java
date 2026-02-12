package com.coffiness.calfit.api.admin.subscription.export;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/subscriptions/export")
public class GET_specs {

    @Test
    void Content_Type이_text_csv이다() {
    }

    @Test
    void CSV_파일에_구독_데이터가_올바르게_포함된다() {
    }

    @Test
    void status_필터가_내보내기_결과에도_적용된다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {
    }

}