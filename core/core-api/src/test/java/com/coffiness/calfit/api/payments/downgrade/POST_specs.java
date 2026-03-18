package com.coffiness.calfit.api.payments.downgrade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.coffiness.calfit.api.CalfitApiTest;
import com.coffiness.calfit.api.fixture.PaymentFixture;
import com.coffiness.calfit.api.v1.response.SubscriptionResponse;
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
@DisplayName("POST /api/v1/payments/downgrade")
class POST_specs {

  @MockBean private TossPaymentClient tossPaymentClient;
  @MockBean private EmailService emailService;

  @Test
  void Enterprise에서_Free로_다운그레이드에_성공한다(@Autowired PaymentFixture paymentFixture) {

    when(tossPaymentClient.issueBillingKey(anyString(), anyString()))
        .thenReturn(
            new BillingKeyResponse(
                "billing-key-456", "customer-key-456", "현대카드", "5678-****-****-1234", "신용", "개인"));
    when(tossPaymentClient.chargeBillingKey(
            anyString(), anyString(), anyString(), anyLong(), anyString()))
        .thenReturn(new PaymentResponse("pay-key-456", "order-456", "DONE", 19900L, null));

    PaymentFixture.PaymentContext context = paymentFixture.setupWorkspace();

    paymentFixture.registerBillingKey(
        context.token(), context.workspaceId(), "customer-key-456", "auth-key-456");
    paymentFixture.upgrade(context.token(), context.workspaceId());

    ApiResponse<Void> response = paymentFixture.downgrade(context.token(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);

    ApiResponse<SubscriptionResponse> subscription =
        paymentFixture.getSubscription(context.token(), context.workspaceId());
    assertThat(subscription.getResult()).isEqualTo(ResultType.SUCCESS);
    assertThat(subscription.getData()).isNotNull();
    assertThat(subscription.getData().planType().name()).isEqualTo("BUSINESS");
  }

  @Test
  void 구독이_없어도_다운그레이드에_성공한다(@Autowired PaymentFixture paymentFixture) {
    PaymentFixture.PaymentContext context = paymentFixture.setupWorkspace();

    ApiResponse<Void> response = paymentFixture.downgrade(context.token(), context.workspaceId());

    assertThat(response.getResult()).isEqualTo(ResultType.SUCCESS);
  }
}
