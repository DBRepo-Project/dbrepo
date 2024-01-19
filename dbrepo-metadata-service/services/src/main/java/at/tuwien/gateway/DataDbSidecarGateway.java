package at.tuwien.gateway;

import at.tuwien.exception.DataDbSidecarException;

public interface DataDbSidecarGateway {
    void importFile(String hostname, Integer port, String filename) throws DataDbSidecarException;

    void exportFile(String hostname, Integer port, String filename) throws DataDbSidecarException;
}
