package app.pastehub.api.storage;

import java.net.URI;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Storage {
    @Bean
    S3Client s3Client(@Value("${pastehub.s3.endpoint}") String endpoint, @Value("${pastehub.s3.access-key}") String accessKey, @Value("${pastehub.s3.secret-key}") String secretKey) {
        return S3Client.builder().endpointOverride(URI.create(endpoint)).region(Region.US_EAST_1).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))).forcePathStyle(true).build();
    }
    @Bean
    S3Presigner s3Presigner(@Value("${pastehub.s3.endpoint}") String endpoint, @Value("${pastehub.s3.access-key}") String accessKey, @Value("${pastehub.s3.secret-key}") String secretKey) {
        return S3Presigner.builder().endpointOverride(URI.create(endpoint)).region(Region.US_EAST_1).credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey))).serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build()).build();
    }
}
