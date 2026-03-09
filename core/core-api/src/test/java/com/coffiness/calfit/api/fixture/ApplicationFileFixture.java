package com.coffiness.calfit.api.fixture;

import com.coffiness.calfit.api.v1.request.CompleteUploadRequest;
import com.coffiness.calfit.api.v1.request.PresignUploadRequest;
import com.coffiness.calfit.api.v1.response.CompleteUploadResponse;
import com.coffiness.calfit.api.v1.response.PresignUploadResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public record ApplicationFileFixture(TestRestTemplate client, ObjectMapper objectMapper) {

  public static ApplicationFileFixture create(Environment environment, ObjectMapper objectMapper) {
    TestRestTemplate client = new TestRestTemplate(new RestTemplateBuilder());
    LocalHostUriTemplateHandler uriTemplateHandler = new LocalHostUriTemplateHandler(environment);
    client.setUriTemplateHandler(uriTemplateHandler);
    return new ApplicationFileFixture(client, objectMapper);
  }

  public ResponseEntity<PresignUploadResponse> presignUpload(
      Long requesterUserId, PresignUploadRequest request) {
    String url =
        String.format(
            "/api/v1/application-files/presign-upload?requesterUserId=%d", requesterUserId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<PresignUploadRequest> entity = new HttpEntity<>(request, headers);
    return client.postForEntity(url, entity, PresignUploadResponse.class);
  }

  public ResponseEntity<CompleteUploadResponse> completeUpload(
      Long requesterUserId, CompleteUploadRequest request) {
    String url =
        String.format("/api/v1/application-files/complete?requesterUserId=%d", requesterUserId);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    HttpEntity<CompleteUploadRequest> entity = new HttpEntity<>(request, headers);
    return client.postForEntity(url, entity, CompleteUploadResponse.class);
  }
}
