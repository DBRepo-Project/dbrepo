package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.monitoring.ReplicationHealthDto;

public interface HealthCheckService {

    /**
     * Performs a local health check for all critical services used by the replication-service:
     * - metadata-service
     * - data-service
     * - replication-service (self)
     * - message broker (RabbitMQ)
     *
     * @return a structured ReplicationHealthDto with per-service health and overall status.
     */
    ReplicationHealthDto checkHealth();
}


