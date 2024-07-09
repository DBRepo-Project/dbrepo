package at.tuwien.gateway;

import at.tuwien.exception.*;

public interface DataDatabaseSidecarGateway {

    /**
     * Imports a given dataset name into the given database.
     * @param hostname The database hostname.
     * @param port The database port.
     * @param filename The dataset name.
     * @throws StorageNotFoundException The dataset name was not found in the storage service.
     * @throws RemoteUnavailableException Connection to the sidecar could not be established.
     * @throws SidecarImportException The sidecar failed to import the dataset.
     */
    void importFile(String hostname, Integer port, String filename) throws StorageNotFoundException,
            RemoteUnavailableException, SidecarImportException;

    /**
     * Exports a given dataset name from the given database.
     * @param hostname The database hostname.
     * @param port The database port.
     * @param filename The dataset name.
     * @throws StorageNotFoundException The dataset name was not found in the storage service.
     * @throws RemoteUnavailableException Connection to the sidecar could not be established.
     * @throws SidecarExportException The sidecar failed to export the dataset.
     */
    void exportFile(String hostname, Integer port, String filename) throws StorageNotFoundException,
            SidecarExportException, RemoteUnavailableException;
}
