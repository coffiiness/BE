package com.coffiness.calfit.domain.applicationFile;

import com.coffiness.calfit.api.v1.request.CompleteUploadRequest;
import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.api.v1.response.CompleteUploadResponse;
import com.coffiness.calfit.api.v1.response.PresignDownloadResponse;
import com.coffiness.calfit.api.v1.response.PresignUploadResponse;
import com.coffiness.calfit.core.enums.UploadStatus;
import com.coffiness.calfit.storage.db.core.application.*;
import com.coffiness.calfit.support.error.CoreException;
import com.coffiness.calfit.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;


import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationFileService {

    private final ApplicationFileRepository applicationFileRepository;
    private final S3Presigner presigner; //presigner URL 제공
    private final S3Client s3Client; //s3에 업로드 검증

    @Value("${s3.applicant-bucket}")
    private String bucket;

    @Value("${s3.key-prefix}")
    private String keyPrefix;

    @Value("${s3.presign-expire-minutes}")
    private long expireMinutes; //presigned URL 유효시간: 10분

    private void validateApplicant(Long requesterUserId, Long applicantId) {
        if (requesterUserId == null || !requesterUserId.equals(applicantId)) {
            throw new CoreException(ErrorType.UNAUTHORIZED);
        }
    }
    private void validateRecruiterCanAccess(Long requesterUserId, Long applicationId){

    }

    @Transactional
    public PresignUploadResponse presignUpload(PresignUploadRequest req, Long requesterUserId) {
        validateApplicant(requesterUserId, req.applicantId());

        // 같은 application + fieldKey 기존 파일 있으면 삭제 상태로 변경
        applicationFileRepository.findByApplicationIdAndFieldKey(req.applicationId(), req.fieldKey())
                .ifPresent(existing -> existing.deleted());

        String ext = extractExt(req.originalFilename());
        String objectKey = String.format(
                "%s/%d/%d/%s/%s%s",
                keyPrefix,
                req.applicantId(),
                req.applicationId(),
                sanitizeFieldKey(req.fieldKey()),
                UUID.randomUUID(),
                ext
        );

        ApplicationFileEntity entity = ApplicationFileEntity.builder()
                .applicationId(req.applicationId())
                .applicantId(req.applicantId())
                .fieldKey(req.fieldKey())
                .objectKey(objectKey)
                .originalFilename(req.originalFilename())
                .contentType(req.contentType())
                .uploadStatus(UploadStatus.PENDING)
                .build();

        applicationFileRepository.save(entity);

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(req.contentType())
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expireMinutes))
                .putObjectRequest(putReq)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignReq);

        return new PresignUploadResponse(entity.getId(), presigned.url().toString(), objectKey, expireMinutes);
    }

    @Transactional
    public CompleteUploadResponse completeUpload(CompleteUploadRequest req, Long requesterUserId) {
        ApplicationFileEntity file = applicationFileRepository.findById(req.fileId())
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        validateApplicant(requesterUserId, file.getApplicantId());

        if (file.getUploadStatus() != UploadStatus.PENDING) {
            throw new CoreException(ErrorType.VALIDATION_ERROR);
        }

        HeadObjectResponse head = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(bucket)
                        .key(file.getObjectKey())
                        .build()
        );

        file.markUploaded(head.contentLength());

        return new CompleteUploadResponse(file.getId(), file.getUploadStatus().name(), file.getSizeBytes());
    }

    @Transactional(readOnly = true)
    public PresignDownloadResponse presignDownload(Long fileId, Long requesterUserId, String role) {
        ApplicationFileEntity file = applicationFileRepository.findById(fileId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND));

        if (file.getUploadStatus() != UploadStatus.ACTIVE) {
            throw new CoreException(ErrorType.VALIDATION_ERROR);
        }

        if ("APPLICANT".equalsIgnoreCase(role)) {
            validateApplicant(requesterUserId, file.getApplicantId());
        } else if ("RECRUITER".equalsIgnoreCase(role)) {
            validateRecruiterCanAccess(requesterUserId, file.getApplicationId());
        } else {
            throw new CoreException(ErrorType.VALIDATION_ERROR);
        }

        String encoded = URLEncoder.encode(file.getOriginalFilename(), StandardCharsets.UTF_8);
        String disposition = "attachment; filename*=UTF-8''" + encoded;

        GetObjectRequest getReq = GetObjectRequest.builder()
                .bucket(bucket)
                .key(file.getObjectKey())
                .responseContentDisposition(disposition)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(expireMinutes))
                .getObjectRequest(getReq)
                .build();

        PresignedGetObjectRequest presigned = presigner.presignGetObject(presignReq);

        return new PresignDownloadResponse(presigned.url().toString(), expireMinutes);
    }

    private String extractExt(String filename) {
        if (filename == null) return "";
        int idx = filename.lastIndexOf('.');
        if (idx == -1) return "";
        return filename.substring(idx); // ".pdf"
    }

    private String sanitizeFieldKey(String fieldKey) {
        return fieldKey == null ? "unknown" : fieldKey.replaceAll("[^a-zA-Z0-9_-]", "_");
    }


}
