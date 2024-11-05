package at.tuwien.gateway;

import at.tuwien.exception.*;

public interface AnalyseServiceGateway {

    /**
     * Imports a given dataset name into the given database.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @param filename   The dataset name.
     * @throws StorageNotFoundException   The dataset name was not found in the storage service.
     * @throws RemoteUnavailableException Connection to the sidecar could not be established.
     * @throws AnalyseServiceException    The analyse service failed to import the dataset.
     */
    void importDataset(Long databaseId, Long tableId, String filename) throws StorageNotFoundException,
            RemoteUnavailableException, AnalyseServiceException;

    /**
     * Exports a given dataset name from the given database.
     *
     * @param databaseId The database id.
     * @param tableId    The table id.
     * @throws StorageNotFoundException   The dataset name was not found in the storage service.
     * @throws RemoteUnavailableException Connection to the sidecar could not be established.
     * @throws AnalyseServiceException    The analyse service failed to export the dataset.
     */
    void exportTable(Long databaseId, Long tableId) throws StorageNotFoundException,
            RemoteUnavailableException, AnalyseServiceException;
}
