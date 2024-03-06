package at.tuwien.service.impl;

import at.tuwien.ExportResource;
import at.tuwien.config.S3Config;
import at.tuwien.exception.FileStorageException;
import at.tuwien.service.StorageService;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

@Log4j2
@Service
public class SeaweedServiceImpl implements StorageService {

    private final S3Config s3Config;
    private final MinioClient minioClient;

    @Autowired
    public SeaweedServiceImpl(S3Config s3Config, MinioClient minioClient) {
        this.s3Config = s3Config;
        this.minioClient = minioClient;
    }

    @Override
    public InputStream getObject(String bucket, String key) throws FileStorageException {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .build());
        } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                 InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                 XmlParserException e) {
            log.error("Failed to find object {} in bucket {}: {}", key, bucket, e.getMessage());
            throw new FileStorageException("Failed to find object " + key + " in bucket " + bucket + ": " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getBytes(String key) throws FileStorageException {
        return getBytes(s3Config.getS3ImportBucket(), key);
    }

    @Override
    public byte[] getBytes(String bucket, String key) throws FileStorageException {
        try {
            return getObject(bucket, key)
                    .readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read bytes from input stream: {}", e.getMessage());
            throw new FileStorageException("Failed to read bytes from input stream: " + e.getMessage(), e);
        }
    }

    @Override
    public ExportResource getResource(String key) throws FileStorageException {
        return getResource(s3Config.getS3ExportBucket(), key);
    }

    @Override
    public ExportResource getResource(String bucket, String key) throws FileStorageException {
        final InputStream stream = getObject(bucket, key);
        return ExportResource.builder()
                .resource(new InputStreamResource(stream))
                .filename(key)
                .build();
    }

    @Override
    public void deleteStaleFiles(String bucketName) throws FileStorageException {
        final List<Item> objects = new LinkedList<>();
        for (Result<Item> result : minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(bucketName)
                .build())) {
            try {
                final Item item = result.get();
                final long diff = item.lastModified().toEpochSecond() - ZonedDateTime.now().minusSeconds(s3Config.getStaleSeconds()).toEpochSecond();
                if (diff <= 0) {
                    log.trace("file {} of bucket {} is due {} second(s)", item.objectName(), bucketName, diff * -1);
                    objects.add(item);
                } else {
                    log.trace("file {} of bucket {} is not yet due for {} second(s)", item.objectName(), bucketName, diff);
                }
            } catch (ErrorResponseException | InsufficientDataException | InternalException | InvalidKeyException |
                     InvalidResponseException | IOException | NoSuchAlgorithmException | ServerException |
                     XmlParserException e) {
                log.error("Failed to retrieve file infos from bucket {}: {}", bucketName, e.getMessage());
                throw new FileStorageException("Failed to retrieve file infos from bucket " + bucketName + ": " + e.getMessage(), e);
            }
        }
        log.debug("deleting files {}", objects.stream().map(Item::objectName).toList());
        final Iterable<Result<DeleteError>> response = minioClient.removeObjects(RemoveObjectsArgs.builder()
                .bucket(bucketName)
                .objects(objects.stream().map(o -> new DeleteObject(o.objectName())).toList())
                .build());
        for (Result<DeleteError> result : response) {
            try {
                result.get();
            } catch (ServerException | InsufficientDataException | ErrorResponseException | IOException |
                     NoSuchAlgorithmException | InvalidKeyException | InvalidResponseException | XmlParserException |
                     InternalException e) {
                log.error("Failed to delete file from bucket {}: {}", bucketName, e.getMessage());
                throw new FileStorageException("Failed to delete file from bucket " + bucketName + ": " + e.getMessage(), e);
            }
        }
        log.info("Deleted {} files", objects.size());
    }

}
