package com.coffiness.calfit.api.payments.query;

import static org.assertj.core.api.Assertions.assertThat;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.v1.response.CardInfoResponse;
import com.coffiness.calfit.api.v1.response.PaymentHistoryResponse;
import com.coffiness.calfit.api.v1.response.SubscriptionResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.support.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@CalfitApiTest
@DisplayName("GET /api/v1/payments - 조회 API")
class GET_specs {

  @MockBean private EmailService emailService;

  @Test
  void 카드_미등록_시_null을_반환한다(@Autowired PaymentFixture paymentFixture) {
    PaymentFixture.PaymentContext context = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> response =
        paymentFixture.getCard(context.token(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 구독_미등록_시_null을_반환한다(@Autowired PaymentFixture paymentFixture) {
    PaymentFixture.PaymentContext context = paymentFixture.setupWorkspace();

    ApiResponse<SubscriptionResponse> response =
        paymentFixture.getSubscription(context.token(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 결제_내역이_없을_때_빈_목록을_반환한다(@Autowired PaymentFixture paymentFixture) {
    PaymentFixture.PaymentContext context = paymentFixture.setupWorkspace();

    ApiResponse<PaymentHistoryResponse[]> response =
        paymentFixture.getHistory(context.token(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }
}
