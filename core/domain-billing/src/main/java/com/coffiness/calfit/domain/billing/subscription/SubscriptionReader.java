package com.coffiness.calfit.domain.billing.subscription;

import com.coffiness.calfit.core.enums.PlanType;
import com.coffiness.calfit.core.enums.SubscriptionStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface SubscriptionReader {

  Optional<Subscription> findById(Long id);

  Optional<Subscription> findByWorkspaceId(String workspaceId);

  List<Subscription> getAll(SubscriptionStatus status, String search, Pageable pageable);

  long countAll(SubscriptionStatus status, String search);

  long countByStatus(SubscriptionStatus status);

  long getMrr();

  List<Subscription> findActiveByPlanType(PlanType planType);
}
