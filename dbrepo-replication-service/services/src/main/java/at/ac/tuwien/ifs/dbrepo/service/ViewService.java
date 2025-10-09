package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;

public interface ViewService {

    void handleViewReplication(ViewNotificationDto viewNotificationDto);
}
