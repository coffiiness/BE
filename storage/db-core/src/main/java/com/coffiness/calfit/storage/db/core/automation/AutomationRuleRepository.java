package com.coffiness.calfit.storage.db.core.automation;

import com.coffiness.calfit.core.enums.AutomationTriggerType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AutomationRuleRepository extends JpaRepository<AutomationRuleEntity, Long> {
  List<AutomationRuleEntity> findByRecruitmentIdAndRecruitmentProcessIdAndTriggerType(
      Long recruitmentId, Long recruitmentProcessId, AutomationTriggerType triggerType);
}
