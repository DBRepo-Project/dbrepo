package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageObjectExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.thirdparty.org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class StorageServiceS3Impl extends DataConnector implements StorageService {

    private final S3Config s3Config;
    private final S3Client s3Client;

    private static final String HASH_SHA256 = "sha256";
    private static final String HASH_SHA1 = "sha1";
    private static final String HASH_MD5 = "md5";

    @Autowired
    public StorageServiceS3Impl(S3Config s3Config, S3Client s3Client) {
        this.s3Config = s3Config;
        this.s3Client = s3Client;
    }

    @Override
    public void putObject(String key, byte[] content) throws StorageObjectExistsException {
        final long start = System.currentTimeMillis();
        try {
            final GetObjectResponse response = s3Client.getObject(GetObjectRequest.builder()
                            .key(key)
                            .bucket(s3Config.getS3Bucket())
                            .build())
                    .response();
            if (matchesAnyHash(response, content)) {
                log.debug("object with key {} already exists", key);
                throw new StorageObjectExistsException("Object already exists");
            }
        } catch (NoSuchKeyException e) {
            /* ignore */
        }
        s3Client.putObject(PutObjectRequest.builder()
                .key(key)
                .bucket(s3Config.getS3Bucket())
                .metadata(new HashMap<>() {{
                    put("sha256", DigestUtils.sha256Hex(content));
                    put("sha1", DigestUtils.sha1Hex(content));
                    put("md5", DigestUtils.md5Hex(content));
                }})
                .build(), RequestBody.fromBytes(content));
        log.atDebug()
                .setMessage("put object in bucket with key " + key + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                .addKeyValue(Constants.ACTION, "s3_put_object")
                .log();
    }

    public boolean matchesAnyHash(GetObjectResponse response, byte[] content) {
        if (response == null) {
            log.trace("response is null");
            return false;
        }
        if (!response.hasMetadata() || response.metadata().isEmpty()) {
            log.trace("response has no metadata");
            return false;
        }
        if (response.metadata().containsKey(HASH_MD5) && response.metadata().get(HASH_MD5).equals(DigestUtils.md5Hex(content))) {
            log.trace("matches md5 hash: {}", response.metadata().get(HASH_MD5));
            return true;
        }
        if (response.metadata().containsKey(HASH_SHA1) && response.metadata().get(HASH_SHA1).equals(DigestUtils.sha1Hex(content))) {
            log.trace("matches sha1 hash: {}", response.metadata().get(HASH_SHA1));
            return true;
        }
        if (response.metadata().containsKey(HASH_SHA256) && response.metadata().get(HASH_SHA256).equals(DigestUtils.sha256Hex(content))) {
            log.trace("matches sha256 hash: {}", response.metadata().get(HASH_SHA256));
            return true;
        }
        return false;
    }

    @Override
    public InputStream getObject(String bucket, String key) throws StorageNotFoundException,
            StorageUnavailableException {
        log.trace("get object from bucket {} with key: {}", bucket, key);
        try {
            final long start = System.currentTimeMillis();
            final InputStream object = s3Client.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            log.atDebug()
                    .setMessage("get object from bucket with key " + key + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                    .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                    .addKeyValue(Constants.ACTION, "s3_get_object")
                    .log();
            return object;
        } catch (NoSuchKeyException | NoSuchBucketException e) {
            log.error("Failed to find object: not found: {}", e.getMessage());
            throw new StorageNotFoundException("Failed to find object: not found: " + e.getMessage(), e);
        } catch (S3Exception e) {
            log.error("Failed to find object: other error: {}", e.getMessage());
            throw new StorageUnavailableException("Failed to find object: other error: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] getBytes(String key) throws StorageNotFoundException, StorageUnavailableException {
        return getBytes(s3Config.getS3Bucket(), key);
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
    public void deleteObject(String key) {
        final long start = System.currentTimeMillis();
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(s3Config.getS3Bucket())
                .key(key)
                .build());
        log.atDebug()
                .setMessage("delete object from bucket with key " + key + " in " + TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - start) + "ms")
                .addKeyValue(Constants.DURATION, System.currentTimeMillis() - start)
                .addKeyValue(Constants.ACTION, "s3_delete_object")
                .log();
    }
}
