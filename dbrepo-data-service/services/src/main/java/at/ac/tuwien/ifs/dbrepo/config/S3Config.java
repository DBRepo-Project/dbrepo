package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Slf4j
@Getter
@Configuration
public class S3Config {

    @Value("${dbrepo.s3.endpoint}")
    private String s3Endpoint;

    @Value("${dbrepo.s3.access.key}")
    private String s3AccessKey;

    @Value("${dbrepo.s3.secret.key}")
    private String s3SecretKey;

    @Value("${dbrepo.s3.bucket}")
    private String s3Bucket;

    @Value("${dbrepo.s3.region}")
    private String s3Region;

    @Value("${dbrepo.sharedFileSystem}")
    private String sharedFileSystem;

    @Bean
    public S3Client s3client() {
        final AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3AccessKey, s3SecretKey));
        return S3Client.builder()
                .region(Region.of(s3Region))
                .endpointOverride(URI.create(s3Endpoint))
                .forcePathStyle(true)
                .credentialsProvider(credentialsProvider)
                .build();
    }


}
