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

    @Value("${dbrepo.spark.hadoop.fs.s3a.endpoint}")
    private String s3aEndpoint;

    @Value("${dbrepo.spark.hadoop.fs.s3a.access.key}")
    private String s3aAccessKey;

    @Value("${dbrepo.spark.hadoop.fs.s3a.secret.key}")
    private String s3aSecretKey;

    @Value("${dbrepo.s3.bucket}")
    private String s3Bucket;

    @Value("${dbrepo.s3.region}")
    private String s3Region;

    @Value("${duckdb.s3.useSsl}")
    private String s3UseSsl;

    @Value("${dbrepo.sharedFileSystem}")
    private String sharedFileSystem;

    @Bean
    public S3Client s3client() {
        final AwsCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(s3aAccessKey, s3aSecretKey));
        return S3Client.builder()
                .region(Region.of(s3Region))
                .endpointOverride(URI.create(s3aEndpoint))
                .forcePathStyle(true)
                .credentialsProvider(credentialsProvider)
                .build();
    }


}
