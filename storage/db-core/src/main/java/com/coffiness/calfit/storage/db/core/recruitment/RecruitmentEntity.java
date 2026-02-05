package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.storage.db.core.TenancyEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 채용 엔티티
 * */
@Entity
@Table(name = "recruitments")
@NoArgsConstructor
@Getter
public class RecruitmentEntity extends TenancyEntity {

    // 워크스페이스 ID
    @Column(name = "workspace_id", nullable = false)
    private Long workspaceId;

    // 채용 작성자 ID
    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    // 채용 제목
    @Column(name = "title", nullable = false)
    private String title;

    // 공고 상태(대기, 진행중, 닫힘)
    @Enumerated(EnumType.STRING)
    @Column(name = "recruitment_status", nullable = false)
    private RecruitmentStatus status;

    // 채용 인원
    @Column(name = "target_count", nullable = false)
    private int targetCount;

    // 채용 시작 날짜
    @Column(name = "start_date")
    private LocalDateTime startDate;

    // 채용 마감 날짜
    @Column(name = "end_date")
    private LocalDateTime endDate;

    @Builder
    public RecruitmentEntity(Long workspaceId, Long creatorId, String title, RecruitmentStatus status, int targetCount, LocalDateTime startDate, LocalDateTime endDate) {
        this.workspaceId = workspaceId;
        this.creatorId = creatorId;
        this.title = title;
        this.status = status;
        this.targetCount = targetCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }
}