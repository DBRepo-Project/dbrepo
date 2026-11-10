package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import software.amazon.awssdk.core.exception.SdkException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class StorageServiceS3Impl implements StorageService {

    private final S3Config s3Config;
    private final S3Client s3Client;

    @Autowired
    public StorageServiceS3Impl(S3Config s3Config, S3Client s3Client) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
    }

    @Override
    public void requireUploadOwner(String key, String owner) throws StorageNotFoundException, StorageUnavailableException {
        if (UploadMetadata.SYSTEM_OWNER.equals(owner)) {
            return;
        }
        if (!UploadMetadata.isUploadKey(key) || owner == null) {
            throw new StorageNotFoundException("Upload not found");
        }
        try {
            final var object = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(s3Config.getS3Bucket()).key(key).build());
            if (!owner.equals(object.metadata().get(UploadMetadata.OWNER))) {
                throw new StorageNotFoundException("Upload not found");
            }
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new StorageNotFoundException("Upload not found", exception);
            }
            throw new StorageUnavailableException("Upload metadata is unavailable", exception);
        } catch (SdkException exception) {
            throw new StorageUnavailableException("Upload metadata is unavailable", exception);
        }
    }

    @Override
    public InputStream getObject(String bucket, String key) throws StorageNotFoundException,
            StorageUnavailableException {
        log.trace("get object with key {} from bucket {}", key, bucket);
        try {
            return s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
        } catch (NoSuchKeyException e) {
            log.error("Failed to find object: not found: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to find object: not found: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("Failed to find object: other error: {}", e.getMessage());
            throw new StorageUnavailableException("Failed to find object: other error: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getBytes(String key) throws StorageNotFoundException, StorageUnavailableException {
        log.trace("get bytes with key {} from bucket {}", key, s3Config.getS3Bucket());
        return getBytes(s3Config.getS3Bucket(), key);
    }

    @Override
    public byte[] getBytes(String bucket, String key) throws StorageNotFoundException, StorageUnavailableException {
        log.trace("get bytes with key {} from bucket {}", key, bucket);
        try (InputStream object = getObject(bucket, key)) {
            return object.readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read bytes from input stream: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to read bytes from input stream: " + e.getMessage(), e);
        }
    }
}
