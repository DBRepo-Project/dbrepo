package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageObjectExistsException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;

import java.io.InputStream;

public interface StorageService {

    /**
     * Uploads content of an object to the S3 backend. It can be later retrieved using the given key.
     *
     * @param key     The key.
     * @param content The content.
     * @throws StorageObjectExistsException The object already exists in the S3 backend with the provided key. It is not necessary to put it again.
     */
    void putObject(String key, byte[] content) throws StorageObjectExistsException;

    /**
     * Loads an object of a bucket from the storage service into an input stream.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The input stream, if successful.
     * @throws StorageUnavailableException The object failed to be loaded from the storage service.
     * @throws StorageNotFoundException    The key was not found in the storage service.
     */
    InputStream getObject(String bucket, String key) throws StorageUnavailableException, StorageNotFoundException;

    /**
     * Loads an object of the default upload bucket from the storage service into a byte array.
     *
     * @param key The object key.
     * @return The byte array.
     * @throws StorageUnavailableException The object failed to be loaded from the storage service.
     * @throws StorageNotFoundException    The key was not found in the storage service.
     */
    byte[] getBytes(String key) throws StorageUnavailableException, StorageNotFoundException;

    /**
     * Loads an object of a bucket from the storage service into a byte array.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The byte array.
     * @throws StorageUnavailableException The object failed to be loaded from the storage service.
     * @throws StorageNotFoundException    The key was not found in the storage service.
     */
    byte[] getBytes(String bucket, String key) throws StorageUnavailableException, StorageNotFoundException;

    /**
     * Deletes an object from the storage service by given key.
     *
     * @param key The object key.
     */
    void deleteObject(String key);
}
