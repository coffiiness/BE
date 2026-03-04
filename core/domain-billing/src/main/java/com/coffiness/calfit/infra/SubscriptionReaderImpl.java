package com.coffiness.calfit.infra;

import com.coffiness.calfit.core.enums.EntityStatus;
import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.domain.billing.subscription.Subscription;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionReader;
import com.coffiness.calfit.storage.db.core.billing.SubscriptionEntity;
import com.coffiness.calfit.storage.db.core.billing.SubscriptionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubscriptionReaderImpl implements SubscriptionReader {

  private final SubscriptionRepository subscriptionRepository;

  @Override
  public Optional<Subscription> findById(Long id) {
    return subscriptionRepository.findById(id).map(this::toSubscription);
  }

  @Override
  public Optional<Subscription> findByWorkspaceId(String workspaceId) {
    return subscriptionRepository
        .findByWorkspaceIdAndStatus(workspaceId, EntityStatus.ACTIVE)
        .map(this::toSubscription);
  }

  @Override
  public List<Subscription> getAll(SubscriptionStatus status, String search, Pageable pageable) {
    String searchParam = (search != null && !search.isBlank()) ? search : null;
    return subscriptionRepository
        .findAllWithFilter(EntityStatus.ACTIVE, status, searchParam, pageable)
        .stream()
        .map(this::toSubscription)
        .toList();
  }

  @Override
  public long countAll(SubscriptionStatus status, String search) {
    String searchParam = (search != null && !search.isBlank()) ? search : null;
    return subscriptionRepository.countWithFilter(EntityStatus.ACTIVE, status, searchParam);
  }

  @Override
  public long countByStatus(SubscriptionStatus status) {
    return subscriptionRepository.countBySubscriptionStatus(EntityStatus.ACTIVE, status);
  }

  @Override
  public long getMrr() {
    return subscriptionRepository.sumMonthlyAmountBySubscriptionStatus(
        EntityStatus.ACTIVE, SubscriptionStatus.ACTIVE);
  }

  @Override
  public List<Subscription> findActiveByPlanType(PlanType planType) {
    return subscriptionRepository
        .findByPlanTypeAndSubscriptionStatusAndStatus(
            planType, SubscriptionStatus.ACTIVE, EntityStatus.ACTIVE)
        .stream()
        .map(this::toSubscription)
        .toList();
  }

  private Subscription toSubscription(SubscriptionEntity entity) {
    return new Subscription(
        entity.getId(),
        entity.getWorkspaceId(),
        entity.getPlanType(),
        entity.getSubscriptionStatus(),
        entity.getMonthlyAmount(),
        entity.getStartDate(),
        entity.getEndDate(),
        entity.getCancelledAt());
  }
}
