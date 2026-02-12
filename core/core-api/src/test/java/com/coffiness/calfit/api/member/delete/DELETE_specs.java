package com.coffiness.calfit.api.member.delete;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("DELETE /api/v1/members/{memberId}")
public class DELETE_specs {

    @Test
    void 정상_삭제_요청_시_204를_반환하고_해당_멤버는_목록에서_제거된다() {
    }

    @Test
    void 존재하지_않는_memberId이면_404_Not_Found를_반환한다() {
    }

    @Test
    void 권한_없는_사용자가_요청하면_403_Forbidden을_반환한다() {
    }

    @Test
    void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다() {
    }

    @Test
    void 자기_자신을_삭제하면_에러를_반환한다() {
    }

    @Test
    void 다른_워크스페이스의_멤버를_삭제하면_에러를_반환한다() {
    }

}