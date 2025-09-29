package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;

import java.util.UUID;

public interface ReplicationForwardingService {

    void forwardTimestampToReplicationTimestamps(TupleWithTimestampsDto created,
                                                 String siteId,
                                                 UUID remoteDatabaseId,
                                                 UUID remoteTableId,
                                                 String replicaUrl);

    void forwardTimestampToForwardingQueue(TupleReplicationTimestampDto dto,
                                           String sourceSiteId,
                                           String originalRoutingKey);

    void forwardTimestampToForwardingQueue(TupleReplicationTimestampDto dto,
                                           DatabaseDto database,
                                           String sourceSiteId);

    String extractSourceSiteId(String value);

    String extractSiteIdFromUrl(String replicaUrl);
}

