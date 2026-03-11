package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentReader {

  List<RecruitmentListInfo> readList(RecruitmentStatus recruitmentStatus, Pageable pageable);

  RecruitmentDetailInfo readDetail(Long recruitmentId);

  Recruitment readById(Long recruitmentId);

  Map<String, Long> countOpenRecruitmentsByTenant();

  List<OpenRecruitmentInfo> readOpenByTenantId(String tenantId);

  List<OpenRecruitmentInfo> readOpenByTenantIdAndSearch(String tenantId, String search);
}
