package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "recruitments")
@NoArgsConstructor
@Getter
public class RecruitmentEntity extends BaseEntity {

    // 워크스페이스 ID
    @Column(nullable = false)
    private Long workspaceId;

    // 채용 작성자 ID
    @Column(nullable = false)
    private Long creatorId;

    // 채용 제목
    @Column(nullable = false)
    private String title;

    // 공고 상태(대기, 진행중, 닫힘)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitmentStatus status;

    // 채용 인원
    @Column(nullable = false)
    private int targetCount;

    private LocalDateTime startDate;

    private LocalDateTime endDate;

    @Builder
    public RecruitmentEntity(Long workspaceId, Long creatorId, String title, RecruitmentStatus status, int targetCount,
            LocalDateTime startDate, LocalDateTime endDate) {
        this.workspaceId = workspaceId;
        this.creatorId = creatorId;
        this.title = title;
        this.status = status;
        this.targetCount = targetCount;
        this.startDate = startDate;
        this.endDate = endDate;
    }

}
