package com.coffiness.calfit.core.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.identity.spi.AwsCredentialsIdentity;
import software.amazon.awssdk.identity.spi.ResolveIdentityRequest;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

class AwsS3ConfigTest {

  private static final String ENDPOINT = "https://synthetic-account.r2.cloudflarestorage.com";
  private static final String BUCKET = "calfit-applicants";

  @ParameterizedTest
  @ValueSource(strings = {"application/pdf", "application/octet-stream"})
  void r2PutPresignsTheSameContentTypeAsTheBrowser(String contentType) {
    withStorage(
        ENDPOINT,
        (client, presigner) -> {
          var put =
              presigner.presignPutObject(
                  PutObjectPresignRequest.builder()
                      .signatureDuration(Duration.ofMinutes(10))
                      .putObjectRequest(
                          PutObjectRequest.builder()
                              .bucket(BUCKET)
                              .key("uploads/1/2/resume/file.pdf")
                              .contentType(contentType)
                              .build())
                      .build());
          assertThat(put.httpRequest().method()).isEqualTo(SdkHttpMethod.PUT);
          assertThat(put.url().getHost()).isEqualTo("synthetic-account.r2.cloudflarestorage.com");
          assertThat(put.url().getPath()).isEqualTo("/" + BUCKET + "/uploads/1/2/resume/file.pdf");
          assertThat(query(put, "X-Amz-Credential")).contains("/auto/s3/aws4_request");
          assertThat(query(put, "X-Amz-Expires")).isEqualTo("600");
          assertThat(put.signedHeaders()).containsOnlyKeys("host", "content-type");
          assertThat(put.signedHeaders().get("content-type")).containsExactly(contentType);
          assertThat(put.httpRequest().rawQueryParameters().keySet())
              .noneMatch(key -> key.toLowerCase(Locale.ROOT).contains("checksum"))
              .doesNotContain("X-Amz-Security-Token");
          assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.of("auto"));
          assertThat(client.serviceClientConfiguration().endpointOverride())
              .contains(URI.create(ENDPOINT));
          assertThat(client.serviceClientConfiguration().requestChecksumCalculation())
              .isEqualTo(RequestChecksumCalculation.WHEN_REQUIRED);
          assertThat(client.serviceClientConfiguration().responseChecksumValidation())
              .isEqualTo(ResponseChecksumValidation.WHEN_REQUIRED);
        });
  }

  @Test
  void r2GetRemainsBrowserExecutableWithDownloadDisposition() {
    withStorage(
        ENDPOINT,
        (client, presigner) -> {
          var get =
              presigner.presignGetObject(
                  GetObjectPresignRequest.builder()
                      .signatureDuration(Duration.ofMinutes(10))
                      .getObjectRequest(
                          GetObjectRequest.builder()
                              .bucket(BUCKET)
                              .key("uploads/resume.pdf")
                              .responseContentDisposition("attachment; filename*=UTF-8''resume.pdf")
                              .build())
                      .build());
          assertThat(get.httpRequest().method()).isEqualTo(SdkHttpMethod.GET);
          assertThat(get.url().getHost()).isEqualTo("synthetic-account.r2.cloudflarestorage.com");
          assertThat(get.url().getPath()).isEqualTo("/" + BUCKET + "/uploads/resume.pdf");
          assertThat(query(get, "X-Amz-Credential")).contains("/auto/s3/aws4_request");
          assertThat(query(get, "X-Amz-Expires")).isEqualTo("600");
          assertThat(query(get, "response-content-disposition")).contains("attachment;");
          assertThat(get.isBrowserExecutable()).isTrue();
          assertThat(get.signedHeaders()).containsOnlyKeys("host");
          assertThat(get.httpRequest().rawQueryParameters().keySet())
              .noneMatch(key -> key.toLowerCase(Locale.ROOT).contains("checksum"));
        });
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  void blankEndpointPreservesAwsEndpointAndRegion(String endpoint) {
    withStorage(
        endpoint,
        (client, presigner) -> {
          assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
          assertThat(client.serviceClientConfiguration().region()).isEqualTo(Region.AP_NORTHEAST_2);
          var get =
              presigner.presignGetObject(
                  GetObjectPresignRequest.builder()
                      .signatureDuration(Duration.ofMinutes(10))
                      .getObjectRequest(
                          GetObjectRequest.builder().bucket(BUCKET).key("resume.pdf").build())
                      .build());
          assertThat(get.url().getHost()).isEqualTo(BUCKET + ".s3.ap-northeast-2.amazonaws.com");
          assertThat(query(get, "X-Amz-Credential")).contains("/ap-northeast-2/s3/aws4_request");
        });
  }

  @Test
  void headObjectUsesTheSameEndpointAndAutoSigningRegion() throws IOException {
    AtomicReference<String> path = new AtomicReference<>();
    AtomicReference<String> authorization = new AtomicReference<>();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/",
        exchange -> {
          path.set(exchange.getRequestURI().getPath());
          authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
          exchange.getResponseHeaders().set("Content-Length", "512");
          exchange.sendResponseHeaders(200, -1);
          exchange.close();
        });
    server.start();
    try {
      withStorage(
          "http://127.0.0.1:" + server.getAddress().getPort(),
          (client, presigner) -> {
            var head =
                client.headObject(
                    HeadObjectRequest.builder().bucket(BUCKET).key("uploads/resume.pdf").build());
            assertThat(head.contentLength()).isEqualTo(512L);
            assertThat(path.get()).isEqualTo("/" + BUCKET + "/uploads/resume.pdf");
            assertThat(authorization.get()).contains("/auto/s3/aws4_request");
          });
    } finally {
      server.stop(0);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"", ENDPOINT})
  void beansStartWithoutResolvingCredentialsOrCallingStorage(String endpoint) {
    DefaultCredentialsProvider credentials = mock(DefaultCredentialsProvider.class);
    try (MockedStatic<DefaultCredentialsProvider> defaults =
        mockStatic(DefaultCredentialsProvider.class)) {
      defaults.when(DefaultCredentialsProvider::create).thenReturn(credentials);
      new ApplicationContextRunner()
          .withUserConfiguration(AwsS3Config.class)
          .withPropertyValues("aws.region=ap-northeast-2", "s3.endpoint=" + endpoint)
          .run(
              context -> {
                assertThat(context)
                    .hasNotFailed()
                    .hasSingleBean(S3Client.class)
                    .hasSingleBean(S3Presigner.class);
                verify(credentials, never()).resolveCredentials();
                verify(credentials, never()).resolveIdentity(any(ResolveIdentityRequest.class));
              });
    }
  }

  private static String query(PresignedRequest request, String name) {
    return request.httpRequest().firstMatchingRawQueryParameter(name).orElseThrow();
  }

  private static void withStorage(String endpoint, BiConsumer<S3Client, S3Presigner> assertions) {
    DefaultCredentialsProvider credentials = mock(DefaultCredentialsProvider.class);
    AwsBasicCredentials syntheticCredentials =
        AwsBasicCredentials.create("synthetic-access-key", "synthetic-secret-key");
    when(credentials.identityType()).thenReturn(AwsCredentialsIdentity.class);
    when(credentials.resolveCredentials()).thenReturn(syntheticCredentials);
    when(credentials.resolveIdentity(any(ResolveIdentityRequest.class)))
        .thenReturn(CompletableFuture.completedFuture(syntheticCredentials));
    try (MockedStatic<DefaultCredentialsProvider> defaults =
        mockStatic(DefaultCredentialsProvider.class)) {
      defaults.when(DefaultCredentialsProvider::create).thenReturn(credentials);
      AwsS3Config config = new AwsS3Config();
      try (S3Client client = config.s3Client("ap-northeast-2", endpoint);
          S3Presigner presigner = config.s3Presigner("ap-northeast-2", endpoint)) {
        assertions.accept(client, presigner);
      }
    }
  }
}
