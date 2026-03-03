package com.coffiness.calfit.domain.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentStatus;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
public interface RecruitmentReader {

  List<RecruitmentListInfo> readList(RecruitmentStatus recruitmentStatus, Pageable pageable);

  RecruitmentDetailInfo readDetail(Long recruitmentId);
}
