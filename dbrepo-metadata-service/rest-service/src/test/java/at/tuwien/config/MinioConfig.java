package at.tuwien.config;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

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

    public void makeBuckets(String... buckets) throws IOException {
        for (String bucket : buckets) {
            try {
                minioClient().makeBucket(MakeBucketArgs.builder()
                        .bucket(bucket)
                        .build());
                log.debug("created bucket {}", bucket);
            } catch (Exception e) {
                log.error("Failed to make bucket {}", bucket);
                throw new IOException("Failed to make bucket: " + e.getMessage());
            }
        }
    }

    public void uploadFile(String bucket, String filename, String key) throws IOException {
        try {
            minioClient().uploadObject(UploadObjectArgs.builder()
                    .bucket(bucket)
                    .filename(filename)
                    .object(key)
                    .build());
            log.debug("uploaded file into bucket {} with key {}", bucket, key);
        } catch (Exception e) {
            log.error("Failed to upload file into bucket {}", bucket);
            throw new IOException("Failed to upload file into bucket: " + e.getMessage());
        }
    }

}
