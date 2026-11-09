package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageObjectExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.i18n.Constants;
import at.ac.tuwien.ifs.dbrepo.service.StorageService;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import software.amazon.awssdk.core.exception.SdkException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
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
    public String putUpload(String originalFilename, Path content, String owner) throws StorageUnavailableException {
        final String key = UploadMetadata.newKey(originalFilename);
        final var metadata = new HashMap<>(UploadMetadata.metadata(originalFilename, owner));
        try {
            final var sha256 = DigestUtils.getSha256Digest();
            final var sha1 = DigestUtils.getSha1Digest();
            final var md5 = DigestUtils.getMd5Digest();
            try (var input = Files.newInputStream(content)) {
                final byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) {
                    sha256.update(buffer, 0, count);
                    sha1.update(buffer, 0, count);
                    md5.update(buffer, 0, count);
                }
            }
            metadata.put(HASH_SHA256, HexFormat.of().formatHex(sha256.digest()));
            metadata.put(HASH_SHA1, HexFormat.of().formatHex(sha1.digest()));
            metadata.put(HASH_MD5, HexFormat.of().formatHex(md5.digest()));
            s3Client.putObject(PutObjectRequest.builder().bucket(s3Config.getS3Bucket()).key(key)
                    .metadata(metadata).overrideConfiguration(configuration -> configuration.putHeader("If-None-Match", "*"))
                    .build(), RequestBody.fromFile(content));
            return key;
        } catch (IOException | SdkException exception) {
            throw new StorageUnavailableException("Upload could not be stored", exception);
        }
    }

    @Override
    public void putObject(String key, byte[] content) throws StorageObjectExistsException {
        final long start = System.currentTimeMillis();
        try {
            final HeadObjectResponse response = s3Client.headObject(HeadObjectRequest.builder()
                    .key(key)
                    .bucket(s3Config.getS3Bucket())
                    .build());
            if (matchesAnyHash(response, content)) {
                log.debug("object with key {} already exists", key);
                throw new StorageObjectExistsException("Object already exists");
            }
        } catch (S3Exception e) {
            // HEAD responses have no error body and may use a generic S3Exception for a missing key.
            if (e.statusCode() != 404) {
                throw e;
            }
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

    public boolean matchesAnyHash(HeadObjectResponse response, byte[] content) {
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
        } catch (SdkException e) {
            log.error("Failed to find object: other error: {}", e.getMessage());
            throw new StorageUnavailableException("Failed to find object: other error: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream getObject(String key) throws StorageNotFoundException, StorageUnavailableException {
        return getObject(s3Config.getS3Bucket(), key);
    }

    @Override
    public byte[] getBytes(String key) throws StorageNotFoundException, StorageUnavailableException {
        return getBytes(s3Config.getS3Bucket(), key);
    }

    @Override
    public byte[] getBytes(String bucket, String key) throws StorageNotFoundException, StorageUnavailableException {
        try (InputStream object = getObject(bucket, key)) {
            return object.readAllBytes();
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
