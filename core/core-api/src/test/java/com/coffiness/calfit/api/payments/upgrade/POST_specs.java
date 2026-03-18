package com.coffiness.calfit.api.payments.upgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.v1.response.CardInfoResponse;
import com.coffiness.calfit.api.v1.response.PaymentResultResponse;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.core.support.response.ResultType;
import com.coffiness.calfit.domain.payment.toss.BillingKeyResponse;
import com.coffiness.calfit.domain.payment.toss.PaymentResponse;
import com.coffiness.calfit.domain.payment.toss.TossPaymentClient;
import com.coffiness.calfit.support.email.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

@CalfitApiTest
@DisplayName("POST /api/v1/payments/upgrade")
class POST_specs {

  @MockBean private TossPaymentClient tossPaymentClient;
  @MockBean private EmailService emailService;

  @Test
  void 빌링키_등록_후_Enterprise_업그레이드에_성공한다(@Autowired PaymentFixture paymentFixture) {

    when(tossPaymentClient.issueBillingKey(anyString(), anyString()))
        .thenReturn(
            new BillingKeyResponse(
                "billing-key-123", "customer-key-123", "삼성카드", "1234-****-****-5678", "신용", "개인"));

    when(tossPaymentClient.chargeBillingKey(
            anyString(), anyString(), anyString(), anyLong(), anyString()))
        .thenReturn(new PaymentResponse("pay-key-123", "order-123", "DONE", 19900L, null));

    PaymentFixture.PaymentContext context = paymentFixture.setupWorkspace();

    ApiResponse<CardInfoResponse> billingKeyResponse =
        paymentFixture.registerBillingKey(
            context.token(), context.workspaceId(), "customer-key-123", "auth-key-123");
    assertThat(billingKeyResponse.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<PaymentResultResponse> response =
        paymentFixture.upgrade(context.token(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }

  @Test
  void 빌링키_없이_업그레이드하면_실패한다(@Autowired PaymentFixture paymentFixture) {
    PaymentFixture.PaymentContext context = paymentFixture.setupWorkspace();

    ApiResponse<PaymentResultResponse> response =
        paymentFixture.upgrade(context.token(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.ERROR);
  }
}
