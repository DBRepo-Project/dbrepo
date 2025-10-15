package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;

public interface ViewService {

    void handleViewReplication(ViewNotificationDto viewNotificationDto);

    /**
     * Rewrites a view SQL so it joins tuple_replication_timestamps and filters using
     * timestamps from the view's creation location.
     */
    String rewriteViewQueryWithReplicationTimestamps(ViewDto viewDto);
}
