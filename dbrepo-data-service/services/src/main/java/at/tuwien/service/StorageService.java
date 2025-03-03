package at.tuwien.service;

import at.tuwien.ExportResourceDto;
import at.tuwien.exception.MalformedException;
import at.tuwien.exception.StorageNotFoundException;
import at.tuwien.exception.StorageUnavailableException;
import at.tuwien.exception.TableMalformedException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.InputStream;
import java.util.List;

public interface StorageService {

    String putObject(byte[] content);

    /**
     * Loads an object of a bucket from the Storage Service into an input stream.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The input stream, if successful.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws StorageNotFoundException    The key was not found in the Storage Service.
     */
    InputStream getObject(String bucket, String key) throws StorageUnavailableException, StorageNotFoundException;

    /**
     * Loads an object of the default upload bucket from the Storage Service into a byte array.
     *
     * @param key The object key.
     * @return The byte array.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws StorageNotFoundException    The key was not found in the Storage Service.
     */
    byte[] getBytes(String key) throws StorageUnavailableException, StorageNotFoundException;

    /**
     * Loads an object of a bucket from the Storage Service into a byte array.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The byte array.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws StorageNotFoundException    The key was not found in the Storage Service.
     */
    byte[] getBytes(String bucket, String key) throws StorageUnavailableException, StorageNotFoundException;

    /**
     * Loads an object of the default export bucket from the Storage Service into an export resource.
     *
     * @param key The object key.
     * @return The export resource, if successful.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws StorageNotFoundException    The key was not found in the Storage Service.
     */
    ExportResourceDto getResource(String key) throws StorageUnavailableException, StorageNotFoundException;

    /**
     * Loads an object of a bucket from the Storage Service into an export resource.
     *
     * @param bucket The bucket name.
     * @param key    The object key.
     * @return The export resource, if successful.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws StorageNotFoundException    The key was not found in the Storage Service.
     */
    ExportResourceDto getResource(String bucket, String key) throws StorageUnavailableException,
            StorageNotFoundException;

    /**
     * Transforms the given dataset into a downloadable dataset.
     *
     * @param data The dataset.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     */
    ExportResourceDto transformDataset(Dataset<Row> data) throws StorageUnavailableException;

    /**
     * Loads the dataset from the Storage Service with given key for a list of provided column names.
     *
     * @param columns    The list of column names.
     * @param key        The key.
     * @param delimiter  The column delimiter, e.g. <code>,</code>
     * @param withHeader If true, the first line contains the column names, otherwise it contains data only.
     * @return The dataset.
     * @throws StorageNotFoundException    The key was not found in the Storage Service.
     * @throws StorageUnavailableException The object failed to be loaded from the Storage Service.
     * @throws MalformedException          The field lengths for the table and dataset are not the same.
     */
    Dataset<Row> loadDataset(List<String> columns, String key, String delimiter, Boolean withHeader) throws StorageNotFoundException,
            StorageUnavailableException, MalformedException, TableMalformedException;
}
