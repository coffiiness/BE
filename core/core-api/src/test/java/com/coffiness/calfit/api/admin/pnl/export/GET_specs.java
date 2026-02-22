package com.coffiness.calfit.api.admin.pnl.export;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("GET /admin/pnl/export")
public class GET_specs {

  @Test
  void 올바르게_요청하면_성공_응답을_반환한다() {}

  @Test
  void 올바르게_요청하면_Content_Type이_Excel_형식이다() {}

  @Test
  void 응답_헤더의_파일명에_조회_월이_포함된다() {}

  @Test
  void Excel_파일에_손익_데이터가_올바르게_포함된다() {}

  @Test
  void 인증되지_않은_사용자는_에러_응답을_반환한다() {}

  @Test
  void month_파라미터_없이_요청하면_현재_월_기준으로_응답한다() {}

  @Test
  void 잘못된_월_형식으로_요청하면_에러_응답을_반환한다() {}

  @Test
  void 데이터가_없는_월을_조회하면_빈_Excel_파일을_반환한다() {}
}
