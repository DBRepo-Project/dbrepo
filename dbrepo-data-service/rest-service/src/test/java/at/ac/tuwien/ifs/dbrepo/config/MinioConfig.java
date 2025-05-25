package at.ac.tuwien.ifs.dbrepo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;

import java.util.List;

@Slf4j
@Configuration
public class MinioConfig {

    public static List<Bucket> listBuckets(S3Client s3Client) {
        return s3Client.listBuckets()
                .buckets();
    }

    public static void makeBucket(S3Client s3Client, String name) {
        if (listBuckets(s3Client).stream().anyMatch(b -> b.name().equals(name))) {
            return;
        }
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket(name)
                .build());
    }

}
