package at.tuwien.gateway;

import at.tuwien.exception.SidecarExportException;
import at.tuwien.exception.SidecarImportException;
import at.tuwien.exception.StorageNotFoundException;

public interface DataDatabaseSidecarGateway {
    void importFile(String hostname, Integer port, String filename) throws SidecarImportException,
            StorageNotFoundException;

    void exportFile(String hostname, Integer port, String filename) throws StorageNotFoundException,
            SidecarExportException;
}
