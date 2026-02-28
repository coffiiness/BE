package com.coffiness.calfit.storage.db.core.application;

import com.coffiness.calfit.core.enums.Gender;
import com.coffiness.calfit.storage.db.core.TenantBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "application_files",
        uniqueConstraints = {
        @UniqueConstraint(name = "uk_object_key", columnNames = {"object_key"})
        }) 
@NoArgsConstructor
@Getter
public class ApplicationFileEntity extends TenantBaseEntity {

    @Column(name = "application_id", nullable = false)
    private Long applicationId;

    // 지원자 ID
    @Column(name = "applicant_id", nullable = false)
    private Long applicantId;

    @Column(name = "object_key", nullable = false)
    private String objectKey;

    @Column(name = "original_filename", nullable = false)
    private String originalFilename;

    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Builder
    public ApplicationFileEntity(Long applicationId, Long applicantId, String objectKey, String originalFilename, String contentType, Long sizeBytes) {
        this.applicationId = applicationId;
        this.applicantId = applicantId;
        this.objectKey = objectKey;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
    }

}
