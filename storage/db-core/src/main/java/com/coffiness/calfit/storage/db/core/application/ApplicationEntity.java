package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.BaseEntity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "Applications") // 지원서
@NoArgsConstructor
@Getter
public class ApplicationEntity extends BaseEntity {

    // 지원자 ID
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;


    // 채용공고 ID
    @Column(name = "recruitment_id", nullable = false)
    private Long recruitmentId;

    // 지원서템플릿 ID
    @Column(name = "template_id", nullable = false)
    private Long templateId;

    // 지원자 이름
    @Column(nullable = false, length = 50)
    private String name;

    // 지원자 성별
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    // 지원자 생년월일
    @Column(nullable = false)
    private LocalDateTime birthDate;

    // 지원자 전화번호
    @Column(nullable = false, length = 20)
    private String phone;

    // 지원자 이메일
    @Column(nullable = false, unique = true, length = 50)
    private String email;

    // 지원서 상세내용
    @Column(columnDefinition = "JSON", nullable = false)
    private String schema;
}
