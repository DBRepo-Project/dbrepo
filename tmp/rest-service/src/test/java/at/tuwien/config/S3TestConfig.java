package at.tuwien.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.List;

@Slf4j
@Getter
@Configuration
public class S3TestConfig {

    @Value("${dbrepo.endpoints.storageService}")
    private String s3Endpoint;

    @Value("${dbrepo.s3.accessKeyId}")
    private String s3AccessKeyId;

    @Value("${dbrepo.s3.secretAccessKey}")
    private String s3SecretAccessKey;

    @Value("${dbrepo.s3.importBucket}")
    private String s3ImportBucket;

    @Value("${dbrepo.s3.exportBucket}")
    private String s3ExportBucket;

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

    public void makeBuckets(List<String> buckets) throws IOException {
        log.trace("creating buckets: {}", buckets);
        for (String bucket : buckets) {
            try {
                if (bucketExists(bucket)) {
                    continue;
                }
            } catch (IOException e) {
                /* ignore */
            }
            try {
                this.s3client()
                        .createBucket(CreateBucketRequest.builder()
                                .bucket(bucket)
                                .build());
                log.debug("created bucket {}", bucket);
            } catch (Exception e) {
                log.error("Failed to create bucket {}: {}", bucket, e.getMessage());
                throw new IOException("Failed to make bucket: " + e.getMessage(), e);
            }
        }
    }

    public boolean bucketExists(String bucket) throws IOException {
        try {
            this.s3client()
                    .headBucket(HeadBucketRequest.builder()
                            .bucket(bucket)
                            .build());
            return true;
        } catch (NoSuchBucketException e) {
            log.error("Bucket {} does not exist: {}", bucket, e.getMessage());
            throw new IOException("Bucket " + bucket + " does not exist: " + e.getMessage(), e);
        }
    }

    public boolean objectExists(String bucket, String key) throws IOException {
        try {
            this.s3client()
                    .headObject(HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build());
            return true;
        } catch (NoSuchKeyException e) {
            log.error("Object {} does not exist in bucket {}: {}", key, bucket, e.getMessage());
            throw new IOException("Object " + key + "does not exist in bucket " + bucket + ": " + e.getMessage(), e);
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
            this.s3client()
                    .putObject(PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(filename)
                            .build(), RequestBody.fromFile(new File(filepath)));
            log.debug("uploaded file into bucket {} with key {}", bucket, filename);
        } catch (Exception e) {
            log.error("Failed to upload file into bucket {}: {}", bucket, e.getMessage());
            throw new IOException("Failed to upload file into bucket " + bucket + ": " + e.getMessage());
        }
    }

}
