package com.coffiness.calfit.api.workspace.create;

import com.coffiness.calfit.api.CalfitApiTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@CalfitApiTest
@DisplayName("POST /workspaces")
public class POST_specs {

    @Test
    void 올바르게_요청하면_201_Created_상태코드를_반환한다() {
    }

    @Test
    void 접근_토큰을_사용하지_않으면_401_Unauthorized_상태코드를_반환한다() {
    }

    @Test
    void 올바르게_요청하면_생성된_워크스페이스에_접근하는_Location_헤더를_반환한다() {
    }

    @Test
    void name_속성이_지정되지_않으면_400_Bad_Request를_반환한다() {
    }

    @Test
    void name_속성이_2자_미만이면_400_Bad_Request를_반환한다() {
    }

    @Test
    void name_속성이_50자_초과이면_400_Bad_Request를_반환한다() {
    }

    @Test
    void contactPhone_속성이_없으면_400_Bad_Request를_반환한다() {
    }

    @Test
    void contactPhone_형식이_올바르지_않으면_400_Bad_Request를_반환한다() {
    }

    @Test
    void employeeScale_속성이_허용된_값이_아니면_400_Bad_Request를_반환한다() {
    }

    @Test
    void role_속성이_허용된_값이_아니면_400_Bad_Request를_반환한다() {
    }

    @Test
    void 이미_워크스페이스를_생성한_사용자가_다시_요청하면_409_Conflict를_반환한다() {
    }

    @Test
    void 워크스페이스_정보를_올바르게_생성한다() {
    }

    @Test
    void 생성한_사용자가_해당_워크스페이스의_관리자로_등록된다() {
    }

}