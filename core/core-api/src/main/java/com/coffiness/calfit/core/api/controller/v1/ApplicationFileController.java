package com.coffiness.calfit.core.api.controller.v1;

import com.coffiness.calfit.api.v1.request.CompleteUploadRequest;
import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.api.v1.response.CompleteUploadResponse;
import com.coffiness.calfit.api.v1.response.PresignDownloadResponse;
import com.coffiness.calfit.api.v1.response.PresignUploadResponse;
import com.coffiness.calfit.domain.applicationFile.ApplicationFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/application-files")
public class ApplicationFileController {

  private final ApplicationFileService service;

  // 임시: requesterUserId는 나중에 JWT에서 꺼내도록 변경!!
  @PostMapping("/presign-upload")
  public PresignUploadResponse presignUpload(
      @RequestParam Long requesterUserId, @RequestBody PresignUploadRequest req) {
    return service.presignUpload(req, requesterUserId);
  }

  @PostMapping("/complete")
  public CompleteUploadResponse complete(
      @RequestParam Long requesterUserId, @RequestBody CompleteUploadRequest req) {
    return service.completeUpload(req, requesterUserId);
  }

  @GetMapping("/{fileId}/presign-download")
  public PresignDownloadResponse presignDownload(
      @PathVariable Long fileId, @RequestParam Long requesterUserId, @RequestParam String role) {
    return service.presignDownload(fileId, requesterUserId, role);
  }

  @GetMapping("/health")
  public String health() {
    return "ok";
  }
}
