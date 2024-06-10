package at.tuwien.gateway;

import at.tuwien.exception.*;

public interface DataDatabaseSidecarGateway {
    void importFile(String hostname, Integer port, String filename) throws StorageNotFoundException,
            RemoteUnavailableException, ServiceException;

    void exportFile(String hostname, Integer port, String filename) throws StorageNotFoundException,
            ServiceException, RemoteUnavailableException;
}
