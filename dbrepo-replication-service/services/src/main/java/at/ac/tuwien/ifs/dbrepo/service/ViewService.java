package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import java.util.Map;
import java.util.UUID;

public interface ViewService {

    void handleViewReplication(ViewNotificationDto viewNotificationDto);

    /**
     * For an incoming replication notification, forward to the local metadata-service endpoint
     * that persists a replicated view while preserving the original ID.
     */
    Map<String, Object> createReplicatedViewLocally(UUID databaseId, ViewNotificationDto viewNotificationDto);
}
