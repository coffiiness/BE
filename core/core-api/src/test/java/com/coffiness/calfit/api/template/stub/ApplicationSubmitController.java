package com.coffiness.calfit.api.template.stub;

import com.coffiness.calfit.api.template.stub.dto.*;
import com.coffiness.calfit.core.support.response.ApiResponse;
import com.coffiness.calfit.support.security.jwt.SecurityUser;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 지원서 제출 컨트롤러 (테스트용 스텁)
 */
@RestController
@RequestMapping("/api/v1/careers")
@RequiredArgsConstructor
public class ApplicationSubmitController {

    private final ApplicationSubmitService service;

    @PostMapping("/{companySlug}/jobs/{jobId}/apply")
    public ResponseEntity<ApiResponse<ApplyResponse>> apply(
            @PathVariable String companySlug,
            @PathVariable Long jobId,
            SecurityUser securityUser,
            @RequestBody ApplyRequest request) {
        ApplyResponse response = service.apply(companySlug, jobId, securityUser.userId(), request);
        return ResponseEntity
                .created(URI.create("/api/v1/careers/" + companySlug + "/jobs/" + jobId + "/applications/"
                        + response.getApplicationId()))
                .body(ApiResponse.success(response));
    }
}