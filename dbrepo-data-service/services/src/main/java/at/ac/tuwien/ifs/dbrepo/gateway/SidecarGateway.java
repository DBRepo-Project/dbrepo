package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.exception.ContainerNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;

public interface SidecarGateway {
    void importCsv(String filename) throws RemoteUnavailableException,
            ContainerNotFoundException, MetadataServiceException;
}
