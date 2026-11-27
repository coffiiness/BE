package com.coffiness.calfit.core.api.config;

import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class AwsS3Config {

  @Bean
  public S3Client s3Client(
      @Value("${aws.region}") String region, @Value("${s3.endpoint:}") String endpoint) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());
    if (!endpoint.isBlank()) {
      builder
          .endpointOverride(URI.create(endpoint.trim()))
          .region(Region.of("auto"))
          .serviceConfiguration(r2Configuration())
          .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
          .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED);
    }
    return builder.build();
  }

  @Bean
  public S3Presigner s3Presigner(
      @Value("${aws.region}") String region, @Value("${s3.endpoint:}") String endpoint) {
    S3Presigner.Builder builder =
        S3Presigner.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create());
    if (!endpoint.isBlank()) {
      builder
          .endpointOverride(URI.create(endpoint.trim()))
          .region(Region.of("auto"))
          // Presigner uses the legacy setting to keep GET URLs browser executable.
          .serviceConfiguration(
              r2Configuration().toBuilder().checksumValidationEnabled(false).build());
    }
    return builder.build();
  }

  private static S3Configuration r2Configuration() {
    return S3Configuration.builder()
        .pathStyleAccessEnabled(true)
        .chunkedEncodingEnabled(false)
        .build();
  }
}
