package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewServiceMariaDbImpl implements ViewService {

    private final ReplicationService replicationService;

    @Override
    public void handleViewReplication(ViewNotificationDto viewNotificationDto) {
        replicationService.sendViewReplicationToInstances(viewNotificationDto);
    }
}

