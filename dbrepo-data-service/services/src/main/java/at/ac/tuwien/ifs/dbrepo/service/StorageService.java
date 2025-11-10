package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.ExportResourceDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableMalformedException;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.io.InputStream;
import java.util.List;

public interface StorageService {

    void putObject(String key, byte[] content);

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

    /**
     * Transforms the given dataset into a downloadable dataset.
     *
     * @param data The dataset.
     * @throws StorageUnavailableException The object failed to be loaded from the storage service.
     */
    ExportResourceDto transformDataset(Dataset<Row> data) throws StorageUnavailableException;

    /**
     * Loads the dataset from the storage service with given key for a list of provided column names.
     *
     * @param columns    The list of column names.
     * @param key        The key.
     * @param delimiter  The column delimiter, e.g. <code>,</code>
     * @param withHeader If true, the first line contains the column names, otherwise it contains data only.
     * @return The dataset.
     * @throws StorageNotFoundException    The key was not found in the storage service.
     * @throws StorageUnavailableException The object failed to be loaded from the storage service.
     * @throws MalformedException          The field lengths for the table and dataset are not the same.
     */
    Dataset<Row> loadDataset(List<String> columns, String key, String delimiter, Boolean withHeader)
            throws StorageNotFoundException, StorageUnavailableException, MalformedException, TableMalformedException;
}
