package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.ViewService;
import at.ac.tuwien.ifs.dbrepo.service.ReplicationService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.client.RestTemplate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ViewServiceMariaDbImpl implements ViewService {

    private final ReplicationService replicationService;
    private final RestTemplate internalRestTemplate;

    @Override
    public void handleViewReplication(ViewNotificationDto viewNotificationDto) {
        replicationService.sendViewReplicationToInstances(viewNotificationDto);
    }

    // Rewriting removed per requirements; the replication service forwards the view as-is

    @Override
    public java.util.Map<String, Object> createReplicatedViewLocally(java.util.UUID databaseId, ViewNotificationDto viewNotificationDto) {
        // Calls metadata-service endpoint to persist replicated view
        return internalRestTemplate.postForObject(
                "/api/v1/database/" + databaseId + "/view/replicated",
                viewNotificationDto,
                java.util.Map.class
        );
    }
}

