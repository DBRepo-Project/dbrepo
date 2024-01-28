package at.tuwien.config;

import io.minio.MinioClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Configuration
public class S3Config {

    @Value("${fda.s3.endpoint}")
    private String s3Endpoint;

    @Value("${fda.s3.accessKeyId}")
    private String s3AccessKeyId;

    @Value("${fda.s3.secretAccessKey}")
    private String s3SecretAccessKey;

    @Value("${fda.s3.importBucket}")
    private String s3ImportBucket;

    @Value("${fda.s3.exportBucket}")
    private String s3ExportBucket;

    @Value("${fda.s3.staleSeconds}")
    private Integer staleSeconds;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(s3Endpoint)
                .credentials(s3AccessKeyId, s3SecretAccessKey)
                .build();
    }

}
