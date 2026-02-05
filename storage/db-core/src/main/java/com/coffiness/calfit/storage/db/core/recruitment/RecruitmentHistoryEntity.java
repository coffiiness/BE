package com.coffiness.calfit.storage.db.core.recruitment;

import com.coffiness.calfit.core.enums.ActionType;
import com.coffiness.calfit.storage.db.core.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/*
*
* */
@Entity
@Table(name = "recruitment_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RecruitmentHistoryEntity extends BaseEntity {

    // 채용 ID
    @Column(name = "recruitment_id", nullable = false)
    private Long recruitmentId;

    // 채용 단계 ID (Optional)
    @Column(name = "recruitment_process_id")
    private Long stageId;

    // 시행자 ID
    @Column(name = "actor_id", nullable = false)
    private Long actorId;

    // 시행 타입
    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private ActionType actionType;

    // 변경한 필드
    @Column(name = "target_field")
    private String targetField;

    // 변경 전 값
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    // 변경 후 값
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    // 시행 이후(Optional)
    @Column(name = "reason")
    private String reason;

    @Builder
    public RecruitmentHistoryEntity(Long recruitmentId, Long stageId, Long actorId, ActionType actionType,
            String targetField, String oldValue, String newValue, String reason) {
        this.recruitmentId = recruitmentId;
        this.stageId = stageId;
        this.actorId = actorId;
        this.actionType = actionType;
        this.targetField = targetField;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.reason = reason;
    }

}
