package at.tuwien.service;

import at.tuwien.ExportResource;
import at.tuwien.exception.FileStorageException;

import java.io.InputStream;

public interface StorageService {

    /**
     * Loads an object of a bucket from the Storage Service into an input stream.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The input stream, if successful.
     * @throws FileStorageException The object failed to be loaded from the Storage Service.
     */
    InputStream getObject(String bucket, String key) throws FileStorageException;

    /**
     * Loads an object of the default upload bucket from the Storage Service into a byte array.
     *
     * @param key The object key.
     * @return The byte array.
     * @throws FileStorageException The object failed to be loaded from the Storage Service.
     */
    byte[] getBytes(String key) throws FileStorageException;

    /**
     * Loads an object of a bucket from the Storage Service into a byte array.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The byte array.
     * @throws FileStorageException The object failed to be loaded from the Storage Service.
     */
    byte[] getBytes(String bucket, String key) throws FileStorageException;

    /**
     * Loads an object of the default export bucket from the Storage Service into an export resource.
     *
     * @param key The object key.
     * @return The export resource, if successful.
     * @throws FileStorageException The object failed to be loaded from the Storage Service.
     */
    ExportResource getResource(String key) throws FileStorageException;

    /**
     * Loads an object of a bucket from the Storage Service into an export resource.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The export resource, if successful.
     * @throws FileStorageException The object failed to be loaded from the Storage Service.
     */
    ExportResource getResource(String bucket, String key) throws FileStorageException;

    /**
     * Deletes files older than an hour from the bucket.
     *
     * @param bucketName The bucket name.
     * @throws FileStorageException The object failed to be loaded from the Storage Service.
     */
    void deleteStaleFiles(String bucketName) throws FileStorageException;
}
