package at.tuwien.config;

import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Value("${fda.minio.endpoint}")
    private String minioEndpoint;

    @Value("${fda.minio.accessKeyId}")
    private String minioAccessKeyId;

    @Value("${fda.minio.secretAccessKey}")
    private String minioSecretAccessKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioEndpoint)
                .credentials(minioAccessKeyId, minioSecretAccessKey)
                .build();
    }

}
