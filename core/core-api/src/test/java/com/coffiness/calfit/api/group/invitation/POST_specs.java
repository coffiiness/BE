package com.coffiness.calfit.api.group.invitation;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /api/v1/invitations")
public class POST_specs {

    @Test
    void 올바른_이메일로_요청_시_초대_생성_후_성공_응답을_반환한다() {
    }

    @Test
    void 잘못된_이메일_형식이면_400_Bad_Request를_반환한다() {
    }

    @Test
    void 이미_가입된_이메일이면_에러_응답을_반환한다() {
    }

    @Test
    void 토큰을_제공하지_않으면_401_Unauthorized를_반환한다() {
    }

    @Test
    void 권한_없는_사용자가_요청하면_403_Forbidden을_반환한다() {
    }

}