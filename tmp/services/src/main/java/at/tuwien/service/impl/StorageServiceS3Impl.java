package at.tuwien.service.impl;

import at.tuwien.ExportResourceDto;
import at.tuwien.config.S3Config;
import at.tuwien.exception.StorageNotFoundException;
import at.tuwien.exception.StorageUnavailableException;
import at.tuwien.service.StorageService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.LinkedList;
import java.util.List;

@Log4j2
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
    public InputStream getObject(String bucket, String key) throws StorageNotFoundException,
            StorageUnavailableException {
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
        return getBytes(s3Config.getS3ImportBucket(), key);
    }

    @Override
    public byte[] getBytes(String bucket, String key) throws StorageNotFoundException, StorageUnavailableException {
        try {
            return getObject(bucket, key)
                    .readAllBytes();
        } catch (IOException e) {
            log.error("Failed to read bytes from input stream: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to read bytes from input stream: " + e.getMessage(), e);
        }
    }

    @Override
    public ExportResourceDto getResource(String key) throws StorageNotFoundException, StorageUnavailableException {
        return getResource(s3Config.getS3ExportBucket(), key);
    }

    @Override
    public ExportResourceDto getResource(String bucket, String key) throws StorageNotFoundException,
            StorageUnavailableException {
        final InputStream stream = getObject(bucket, key);
        return ExportResourceDto.builder()
                .resource(new InputStreamResource(stream))
                .filename(key)
                .build();
    }
}
