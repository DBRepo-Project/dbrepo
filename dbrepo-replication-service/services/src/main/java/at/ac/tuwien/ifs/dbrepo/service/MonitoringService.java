package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationMonitoringDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.MetadataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import org.springframework.http.HttpStatus;

import java.util.UUID;

public interface MonitoringService {

    /**
     * Health/status check that loads a single database and all its tables for monitoring.
     *
     * @param databaseId The database to check
     */
    ReplicationMonitoringDatabaseDto status(UUID databaseId)
            throws RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException;

    /**
     * Determine an aggregated health status string based on the result of {@link #status(UUID)}.
     *
     * @param databaseStatus The monitoring result for a database.
     * @return Aggregated status (e.g. healthy, degraded, unreachable).
     */
    String deriveGlobalStatus(ReplicationMonitoringDatabaseDto databaseStatus);

    /**
     * Map the aggregated status to an appropriate HTTP status code for presentation.
     *
     * @param status Aggregated status string.
     * @return Matching HTTP status.
     */
    HttpStatus mapStatusToHttp(String status);
}

