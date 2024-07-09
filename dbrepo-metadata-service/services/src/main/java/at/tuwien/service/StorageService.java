package at.tuwien.service;

import at.tuwien.exception.StorageNotFoundException;
import at.tuwien.exception.StorageUnavailableException;

import java.io.InputStream;

public interface StorageService {

    /**
     * Loads an object of a bucket from the Storage Service into an input stream.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The input stream, if successful.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws StorageNotFoundException The object could not be found in the Storage Service.
     */
    InputStream getObject(String bucket, String key) throws StorageNotFoundException,
            StorageUnavailableException;

    /**
     * Loads an object of the default upload bucket from the Storage Service into a byte array.
     *
     * @param key The object key.
     * @return The byte array.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws StorageNotFoundException The object could not be found in the Storage Service.
     */
    byte[] getBytes(String key) throws StorageUnavailableException, StorageNotFoundException;

    /**
     * Loads an object of a bucket from the Storage Service into a byte array.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The byte array.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws StorageNotFoundException The object could not be found in the Storage Service.
     */
    byte[] getBytes(String bucket, String key) throws StorageNotFoundException, StorageUnavailableException;
}
