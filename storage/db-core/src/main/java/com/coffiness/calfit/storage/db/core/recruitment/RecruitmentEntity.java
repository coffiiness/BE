package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.RecruitmentStatus;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recruitments")
public class RecruitmentEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long workspaceId; // 워크스페이스 ID

    @Column(nullable = false)
    private Long creatorId; // 채용 작성자 ID

    @Column(nullable = false)
    private String title; // 채용 제목

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecruitmentStatus status; // 공고 상태(대기, 진행중, 닫힘)

    @Column(nullable = false)
    private int targetCount; // 채용 인원

    private LocalDateTime startDate;

    private LocalDateTime endDate;

}
