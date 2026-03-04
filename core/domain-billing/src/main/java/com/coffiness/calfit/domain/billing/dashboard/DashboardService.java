package com.coffiness.calfit.domain.billing.dashboard;

import com.coffiness.calfit.core.enums.SubscriptionStatus;
import com.coffiness.calfit.domain.billing.cost.CostReader;
import com.coffiness.calfit.domain.billing.cost.MonthlyCostTotal;
import com.coffiness.calfit.domain.billing.invoice.InvoiceReader;
import com.coffiness.calfit.domain.billing.invoice.MonthlyRevenue;
import com.coffiness.calfit.domain.billing.subscription.SubscriptionReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

  private final SubscriptionReader subscriptionReader;
  private final InvoiceReader invoiceReader;
  private final CostReader costReader;

  public DashboardSummary getSummary(int year, int month) {
    long mrr = subscriptionReader.getMrr();
    long activeCount = subscriptionReader.countByStatus(SubscriptionStatus.ACTIVE);
    long trialCount = subscriptionReader.countByStatus(SubscriptionStatus.TRIAL);
    long cancelledCount = subscriptionReader.countByStatus(SubscriptionStatus.CANCELLED);

    List<MonthlyRevenue> revenueHistory = invoiceReader.getMonthlyRevenue();
    double mrrGrowth = computeGrowth(revenueHistory, year, month);

    Map<com.coffiness.calfit.core.enums.CostCategory, Long> costSummary =
        costReader.getSummaryByCategory(year, month);
    long totalCost = costSummary.values().stream().mapToLong(Long::longValue).sum();

    List<MonthlyCostTotal> costHistory = costReader.getMonthlyTrend();
    double costGrowth = computeCostGrowth(costHistory, year, month);

    long subscribers = activeCount + trialCount;
    long newThisMonth = 0;

    return new DashboardSummary(
        mrr, mrrGrowth, subscribers, newThisMonth, 0.0, 0.0, totalCost, costGrowth);
  }

  public List<RevenueCostTrend> getRevenueCostTrend() {
    List<MonthlyRevenue> revenues = invoiceReader.getMonthlyRevenue();
    List<MonthlyCostTotal> costs = costReader.getMonthlyTrend();

    Map<String, Long> revenueMap =
        revenues.stream()
            .collect(Collectors.toMap(r -> r.year() + "-" + r.month(), MonthlyRevenue::amount));
    Map<String, Long> costMap =
        costs.stream()
            .collect(Collectors.toMap(c -> c.year() + "-" + c.month(), MonthlyCostTotal::amount));

    // Build trend for last 8 months
    List<RevenueCostTrend> trend = new ArrayList<>();
    LocalDate now = LocalDate.now();
    for (int i = 7; i >= 0; i--) {
      LocalDate d = now.minusMonths(i);
      String key = d.getYear() + "-" + d.getMonthValue();
      trend.add(
          new RevenueCostTrend(
              d.getYear(),
              d.getMonthValue(),
              revenueMap.getOrDefault(key, 0L),
              costMap.getOrDefault(key, 0L)));
    }
    return trend;
  }

  public List<PlanDistribution> getPlanDistribution() {
    List<PlanDistribution> result = new ArrayList<>();
    for (SubscriptionStatus status : SubscriptionStatus.values()) {
      long count = subscriptionReader.countByStatus(status);
      if (count > 0) {
        result.add(new PlanDistribution(status.name(), count));
      }
    }
    return result;
  }

  private double computeGrowth(List<MonthlyRevenue> history, int year, int month) {
    if (history.size() < 2) return 0.0;
    long current =
        history.stream()
            .filter(r -> r.year() == year && r.month() == month)
            .mapToLong(MonthlyRevenue::amount)
            .sum();
    LocalDate prevDate = LocalDate.of(year, month, 1).minusMonths(1);
    long previous =
        history.stream()
            .filter(r -> r.year() == prevDate.getYear() && r.month() == prevDate.getMonthValue())
            .mapToLong(MonthlyRevenue::amount)
            .sum();
    if (previous == 0) return 0.0;
    return Math.round(((double) (current - previous) / previous * 100) * 10.0) / 10.0;
  }

  private double computeCostGrowth(List<MonthlyCostTotal> history, int year, int month) {
    if (history.size() < 2) return 0.0;
    long current =
        history.stream()
            .filter(c -> c.year() == year && c.month() == month)
            .mapToLong(MonthlyCostTotal::amount)
            .sum();
    LocalDate prevDate = LocalDate.of(year, month, 1).minusMonths(1);
    long previous =
        history.stream()
            .filter(c -> c.year() == prevDate.getYear() && c.month() == prevDate.getMonthValue())
            .mapToLong(MonthlyCostTotal::amount)
            .sum();
    if (previous == 0) return 0.0;
    return Math.round(((double) (current - previous) / previous * 100) * 10.0) / 10.0;
  }
}
