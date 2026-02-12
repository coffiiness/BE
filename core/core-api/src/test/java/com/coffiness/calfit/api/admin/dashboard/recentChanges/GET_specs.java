package com.coffiness.calfit.api.admin.dashboard.recentChanges;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/dashboard/recent-changes")
public class GET_specs {

    @Test
    void 올바르게_요청하면_200_OK_상태코드를_반환한다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {
    }

    @Test
    void 변동_내역을_최신순으로_정렬하여_반환한다() {
    }

    @Test
    void 변동_유형_changeType을_올바르게_반환한다() {
    }

    @Test
    void 변동_내역이_없으면_빈_리스트를_반환한다() {
    }

    @Test
    void 관리자가_아닌_사용자는_403_Forbidden을_반환한다() {
    }

}