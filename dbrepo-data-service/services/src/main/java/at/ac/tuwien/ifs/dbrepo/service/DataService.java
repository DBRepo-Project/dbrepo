package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.classic.Dataset;

import java.util.List;

public interface DataService {

    /**
     * Loads the dataset from the storage service with given key for a list of provided column names.
     *
     * @param key        The key.
     * @param delimiter  The column delimiter, e.g. <code>,</code>
     * @param withHeader If true, the first line contains the column names, otherwise it contains data only.
     * @return The dataset.
     * @throws StorageNotFoundException    The key was not found in the storage service.
     * @throws StorageUnavailableException The object failed to be loaded from the storage service.
     * @throws MalformedException          The field lengths for the table and dataset are not the same.
     */
    Dataset<Row> getCsv(List<String> columns, String key, String delimiter, Boolean withHeader)
            throws StorageNotFoundException, StorageUnavailableException, MalformedException, TableMalformedException;

    Dataset<Row> getSubset(Database database, String query) throws QueryMalformedException, TableNotFoundException;

    Dataset<Row> getSubsetAsJson(Database database, String query) throws QueryMalformedException, TableNotFoundException;

    Dataset<Row> getSubsetAsCsv(Database database, String query) throws QueryMalformedException, TableNotFoundException;
}
