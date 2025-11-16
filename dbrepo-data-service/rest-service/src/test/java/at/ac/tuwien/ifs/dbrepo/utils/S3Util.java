package at.ac.tuwien.ifs.dbrepo.utils;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import lombok.extern.log4j.Log4j2;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class S3Util {

    public static void cleanBucket(S3Client s3Client, S3Config s3Config) {
        if (s3Client.listBuckets().buckets().stream().noneMatch(b -> b.name().equals(s3Config.getS3Bucket()))) {
            log.warn("Bucket {} not found", s3Config.getS3Bucket());
            s3Client.createBucket(CreateBucketRequest.builder()
                    .bucket(s3Config.getS3Bucket())
                    .build());
            log.info("Bucket {} created", s3Config.getS3Bucket());
        } else {
            final Set<String> keys = s3Client.listObjects(ListObjectsRequest.builder()
                            .bucket(s3Config.getS3Bucket())
                            .build())
                    .contents()
                    .stream()
                    .map(S3Object::key)
                    .collect(Collectors.toSet());
            log.trace("found {} keys: {}", keys.size(), keys);
            if (!keys.isEmpty()) {
                s3Client.deleteObjects(DeleteObjectsRequest.builder()
                        .bucket(s3Config.getS3Bucket())
                        .delete(Delete.builder()
                                .objects(keys.stream()
                                        .map(k -> ObjectIdentifier.builder()
                                                .key(k)
                                                .build())
                                        .toList())
                                .build())
                        .build());
                log.info("Bucket {} cleaned", s3Config.getS3Bucket());
            }
        }
    }

}
