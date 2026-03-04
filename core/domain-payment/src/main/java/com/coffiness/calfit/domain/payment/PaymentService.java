package com.coffiness.calfit.domain.payment;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.domain.billing.event.PaymentCompletedEvent;
import com.coffiness.calfit.domain.billing.event.PaymentFailedEvent;
import com.coffiness.calfit.domain.billing.invoice.InvoiceStore;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionReader;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionStore;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyInfo;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyReader;
import com.coffiness.calfit.domain.payment.billingkey.BillingKeyStore;
import com.coffiness.calfit.domain.payment.payment.Payment;
import com.coffiness.calfit.domain.payment.payment.PaymentReader;
import com.coffiness.calfit.domain.payment.payment.PaymentStore;
import com.coffiness.calfit.domain.payment.toss.BillingKeyResponse;
import com.coffiness.calfit.domain.payment.toss.PaymentResponse;
import com.coffiness.calfit.domain.payment.toss.TossPaymentClient;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

  private static final Long ENTERPRISE_MONTHLY_AMOUNT = 19_900L;
  private static final String ORDER_NAME = "Coffiiness Enterprise 월 구독";
  private static final int MAX_RETRY_COUNT = 2;

  private final BillingKeyReader billingKeyReader;
  private final BillingKeyStore billingKeyStore;
  private final PaymentReader paymentReader;
  private final PaymentStore paymentStore;
  private final TossPaymentClient tossPaymentClient;
  private final SubscriptionReader subscriptionReader;
  private final SubscriptionStore subscriptionStore;
  private final InvoiceStore invoiceStore;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public BillingKeyInfo registerBillingKey(String workspaceId, String customerKey, String authKey) {
    BillingKeyResponse response = tossPaymentClient.issueBillingKey(customerKey, authKey);
    return billingKeyStore.save(
        workspaceId,
        response.billingKey(),
        response.customerKey(),
        response.cardCompany(),
        response.cardNumber(),
        response.cardType(),
        response.ownerType());
  }

  @Transactional
  public Optional<Payment> upgradeToEnterprise(String workspaceId) {
    BillingKeyInfo billingKeyInfo =
        billingKeyReader
            .findActiveByWorkspaceId(workspaceId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    Subscription subscription =
        subscriptionReader
            .findByWorkspaceId(workspaceId)
            .orElseGet(() -> subscriptionStore.createTrialSubscription(workspaceId));

    subscriptionStore.activate(subscription.id(), PlanType.ENTERPRISE, ENTERPRISE_MONTHLY_AMOUNT);

    LocalDate now = LocalDate.now();
    if (paymentReader.existsSuccessfulPaymentInMonth(
        workspaceId, now.getYear(), now.getMonthValue())) {
      log.info("[Payment] 이번 달 이미 결제 완료. 구독만 활성화: workspaceId={}", workspaceId);
      return Optional.empty();
    }

    var invoice =
        invoiceStore.createPendingInvoice(
            subscription.id(), workspaceId, now.getYear(), now.getMonthValue());

    String orderId = generateOrderId(workspaceId);
    Payment payment =
        paymentStore.createPayment(
            workspaceId,
            subscription.id(),
            invoice.id(),
            billingKeyInfo.billingKey(),
            orderId,
            ENTERPRISE_MONTHLY_AMOUNT);

    PaymentResponse tossResponse =
        tossPaymentClient.chargeBillingKey(
            billingKeyInfo.billingKey(),
            billingKeyInfo.customerKey(),
            orderId,
            ENTERPRISE_MONTHLY_AMOUNT,
            ORDER_NAME);

    if ("DONE".equals(tossResponse.status())) {
      payment = paymentStore.markSuccess(payment.id(), tossResponse.paymentKey());
      eventPublisher.publishEvent(
          PaymentCompletedEvent.of(invoice.id(), subscription.id(), ENTERPRISE_MONTHLY_AMOUNT));
    } else {
      String failMessage =
          tossResponse.failureMessage() != null ? tossResponse.failureMessage() : "결제 실패";
      payment = paymentStore.markFailed(payment.id(), failMessage, LocalDateTime.now().plusDays(3));
      eventPublisher.publishEvent(
          PaymentFailedEvent.of(invoice.id(), subscription.id(), failMessage));
    }

    return Optional.of(payment);
  }

  @Transactional
  public void downgradeToFree(String workspaceId) {
    Subscription subscription =
        subscriptionReader
            .findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    subscriptionStore.cancel(subscription.id());
    subscriptionStore.activate(subscription.id(), PlanType.BUSINESS, 0L);
  }

  @Transactional
  public Payment processMonthlyBilling(String workspaceId) {
    BillingKeyInfo billingKeyInfo =
        billingKeyReader
            .findActiveByWorkspaceId(workspaceId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    Subscription subscription =
        subscriptionReader
            .findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    var invoice =
        invoiceStore.createPendingInvoice(
            subscription.id(),
            workspaceId,
            LocalDate.now().getYear(),
            LocalDate.now().getMonthValue());

    String orderId = generateOrderId(workspaceId);
    Payment payment =
        paymentStore.createPayment(
            workspaceId,
            subscription.id(),
            invoice.id(),
            billingKeyInfo.billingKey(),
            orderId,
            ENTERPRISE_MONTHLY_AMOUNT);

    PaymentResponse tossResponse =
        tossPaymentClient.chargeBillingKey(
            billingKeyInfo.billingKey(),
            billingKeyInfo.customerKey(),
            orderId,
            ENTERPRISE_MONTHLY_AMOUNT,
            ORDER_NAME);

    if ("DONE".equals(tossResponse.status())) {
      payment = paymentStore.markSuccess(payment.id(), tossResponse.paymentKey());
      eventPublisher.publishEvent(
          PaymentCompletedEvent.of(invoice.id(), subscription.id(), ENTERPRISE_MONTHLY_AMOUNT));
    } else {
      String failMessage =
          tossResponse.failureMessage() != null ? tossResponse.failureMessage() : "결제 실패";
      payment = paymentStore.markFailed(payment.id(), failMessage, LocalDateTime.now().plusDays(3));
      eventPublisher.publishEvent(
          PaymentFailedEvent.of(invoice.id(), subscription.id(), failMessage));
    }

    return payment;
  }

  @Transactional
  public Payment retryFailedPayment(Long paymentId) {
    Payment payment =
        paymentReader.findById(paymentId).orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    BillingKeyInfo billingKeyInfo =
        billingKeyReader
            .findActiveByWorkspaceId(payment.workspaceId())
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

    PaymentResponse tossResponse =
        tossPaymentClient.chargeBillingKey(
            billingKeyInfo.billingKey(),
            billingKeyInfo.customerKey(),
            payment.orderId(),
            payment.amount(),
            ORDER_NAME);

    if ("DONE".equals(tossResponse.status())) {
      Payment result = paymentStore.markSuccess(paymentId, tossResponse.paymentKey());
      eventPublisher.publishEvent(
          PaymentCompletedEvent.of(
              payment.invoiceId(), payment.subscriptionId(), payment.amount()));
      return result;
    }

    String failMessage =
        tossResponse.failureMessage() != null ? tossResponse.failureMessage() : "결제 재시도 실패";

    if (payment.retryCount() >= MAX_RETRY_COUNT) {
      return paymentStore.markFinalFailed(paymentId, failMessage);
    }

    long daysToNext = payment.retryCount() == 0 ? 3 : 1;
    return paymentStore.markFailed(
        paymentId, failMessage, LocalDateTime.now().plusDays(daysToNext));
  }

  public BillingKeyInfo getRegisteredCard(String workspaceId) {
    return billingKeyReader.findActiveByWorkspaceId(workspaceId).orElse(null);
  }

  public Subscription getSubscription(String workspaceId) {
    return subscriptionReader.findByWorkspaceId(workspaceId).orElse(null);
  }

  public List<Payment> getPaymentHistory(String workspaceId, int page, int size) {
    return paymentReader.findByWorkspaceId(workspaceId, page, size);
  }

  public long getPaymentHistoryCount(String workspaceId) {
    return paymentReader.countByWorkspaceId(workspaceId);
  }

  @Transactional
  public void suspendSubscription(String workspaceId) {
    Subscription subscription =
        subscriptionReader
            .findByWorkspaceId(workspaceId)
            .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));
    subscriptionStore.updateStatus(subscription.id(), SubscriptionStatus.SUSPENDED);
  }

  private String generateOrderId(String workspaceId) {
    return "CFN-"
        + workspaceId.substring(0, Math.min(8, workspaceId.length()))
        + "-"
        + UUID.randomUUID().toString().substring(0, 8);
  }
}
