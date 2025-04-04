package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

@Log4j2
@Getter
@Configuration
public class S3Config {

    @Value("${dbrepo.endpoints.storageService}")
    private String s3Endpoint;

    @Value("${dbrepo.s3.accessKeyId}")
    private String s3AccessKeyId;

    @Value("${dbrepo.s3.secretAccessKey}")
    private String s3SecretAccessKey;

    @Value("${dbrepo.s3.bucket}")
    private String s3Bucket;

    @Value("${dbrepo.s3.maxAge}")
    private Integer maxAge;

    @Bean
    public S3Client s3client() {
        final AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3AccessKeyId, s3SecretAccessKey));
        return S3Client.builder()
                .region(Region.EU_WEST_1)
                .endpointOverride(URI.create(s3Endpoint))
                .forcePathStyle(true)
                .credentialsProvider(credentialsProvider)
                .build();
    }


}
