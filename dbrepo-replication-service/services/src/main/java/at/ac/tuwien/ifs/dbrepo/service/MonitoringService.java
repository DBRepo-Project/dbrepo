package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;

import java.util.Map;
import java.util.UUID;

public interface MonitoringService {

    /**
     * Health/status check that loads a single database and all its tables for monitoring.
     * @param databaseId The database to check
     */
    ReplicationMonitoringDatabaseDto status(UUID databaseId) throws RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException;
}


