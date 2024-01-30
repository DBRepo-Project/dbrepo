package at.tuwien.gateway;

import at.tuwien.exception.DataDbSidecarException;
import at.tuwien.exception.DataProcessingException;

public interface DataDbSidecarGateway {
    void importFile(String hostname, Integer port, String filename) throws DataDbSidecarException, DataProcessingException;

    void exportFile(String hostname, Integer port, String filename) throws DataDbSidecarException, DataProcessingException;
}
