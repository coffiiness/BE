package com.coffiness.calfit.api.admin.invoice.list;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/invoices")
public class GET_specs {

  @Test
  void 상태_필터_PAID_UNPAID_OVERDUE가_올바르게_동작한다() {}

  @Test
  void 페이지네이션_및_발행일_역순_정렬이_동작한다() {}

  @Test
  void 접근_토큰을_사용하지_않으면_401_Unauthorized를_반환한다() {}

  @Test
  void 인보이스_ID는_INV_YYYY_순번4자리_형식을_따른다() {}

  @Test
  void 관리자가_아닌_사용자는_403_Forbidden을_반환한다() {}

  @Test
  void 인보이스가_없으면_빈_리스트를_반환한다() {}
}
