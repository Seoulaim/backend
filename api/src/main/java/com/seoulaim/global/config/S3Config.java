package com.seoulaim.global.config;

import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

  @Bean
  public S3Client s3Client(StorageProperties properties) {
    S3ClientBuilder builder =
        S3Client.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(credentialsProvider(properties))
            .serviceConfiguration(serviceConfiguration(properties));
    if (StringUtils.hasText(properties.endpoint())) {
      builder.endpointOverride(URI.create(properties.endpoint()));
    }
    return builder.build();
  }

  @Bean
  public S3Presigner s3Presigner(StorageProperties properties) {
    S3Presigner.Builder builder =
        S3Presigner.builder()
            .region(Region.of(properties.region()))
            .credentialsProvider(credentialsProvider(properties))
            .serviceConfiguration(serviceConfiguration(properties));
    if (StringUtils.hasText(properties.endpoint())) {
      builder.endpointOverride(URI.create(properties.endpoint()));
    }
    return builder.build();
  }

  private AwsCredentialsProvider credentialsProvider(StorageProperties properties) {
    if (properties.hasStaticCredentials()) {
      return StaticCredentialsProvider.create(
          AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    }
    return DefaultCredentialsProvider.builder().build();
  }

  private S3Configuration serviceConfiguration(StorageProperties properties) {
    return S3Configuration.builder().pathStyleAccessEnabled(properties.pathStyleAccess()).build();
  }
}
