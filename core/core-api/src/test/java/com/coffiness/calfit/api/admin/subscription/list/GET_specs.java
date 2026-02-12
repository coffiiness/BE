package com.coffiness.calfit.api.admin.subscription.list;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/subscriptions")
public class GET_specs {

    @Test
    void 올바르게_요청하면_200_OK_상태코드를_반환한다() {
    }

    @Test
    void status가_ACTIVE이면_활성_구독만_반환한다() {
    }

    @Test
    void search_매개변수로_회사명_도메인_검색이_올바르게_동작한다() {
    }

    @Test
    void 페이지네이션이_올바르게_동작한다() {
    }

    @Test
    void 구독_시작일_역순으로_정렬된다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {
    }

    @Test
    void 관리자가_아닌_사용자는_403_Forbidden을_반환한다() {
    }

    @Test
    void status와_search를_동시에_사용해도_올바르게_동작한다() {
    }

    @Test
    void 검색_결과가_없으면_빈_리스트를_반환한다() {
    }

}