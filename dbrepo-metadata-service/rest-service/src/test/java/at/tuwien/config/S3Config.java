package at.tuwien.config;

import io.minio.*;
import io.minio.errors.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

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

    public void makeBuckets(String... buckets) throws IOException {
        for (String bucket : buckets) {
            if (this.bucketExists(bucket)) {
                continue;
            }
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

    public boolean bucketExists(String bucket) throws IOException {
        try {
            final boolean result = minioClient().bucketExists(BucketExistsArgs.builder()
                    .bucket(bucket)
                    .build());
            log.trace("bucket {} does {}exist", bucket, result ? "" : "not");
            return result;
        } catch (Exception e) {
            log.error("Failed to check bucket {} existence", bucket);
            throw new IOException("Failed to check bucket " + bucket + "existence: " + e.getMessage());
        }
    }

    public boolean objectExists(String bucket, String key) {
        try {
            final StatObjectResponse response = minioClient().statObject(StatObjectArgs.builder()
                    .object(key)
                    .bucket(bucket)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void uploadFile(String bucket, String filepath, String filename) throws IOException {
        final File file = new File(filepath);
        if (!file.exists()) {
            log.error("Failed to upload file at path {}: does not exist", filepath);
            throw new IOException("Failed to upload file at path " + filepath + ": does not exist");
        }
        if (!file.isFile()) {
            log.error("Failed to upload file at path {}: is not a file", filepath);
            throw new IOException("Failed to upload file at path " + filepath + ": is not a file");
        }
        try {
            minioClient().uploadObject(UploadObjectArgs.builder()
                    .bucket(bucket)
                    .filename(filepath)
                    .object(filename)
                    .build());
            log.debug("uploaded file into bucket {} with key {}", bucket, filename);
        } catch (Exception e) {
            log.error("Failed to upload file into bucket {}: {}", bucket, e.getMessage());
            throw new IOException("Failed to upload file into bucket " + bucket + ": " + e.getMessage());
        }
    }

}
