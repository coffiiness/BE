package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentProcessType;
import com.coffiness.calfit.storage.db.core.TenancyEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
 * 채용 단계 엔티티
 * */
@Entity
@Table(name = "recruitment_processes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RecruitmentProcessEntity extends TenancyEntity {

    // 채용 ID
    @Column(name = "recruitment_id", nullable = false)
    private Long recruitmentId;

    // 채용 단계명
    @Column(name = "stage_name", nullable = false)
    private String stageName;

    // 채용 단계 순서
    @Column(name = "stage_step", nullable = false)
    private Integer stageStep;

    // 채용 단계 유형
    @Enumerated(EnumType.STRING)
    @Column(name = "stage_type", nullable = false)
    private RecruitmentProcessType stageType;

}