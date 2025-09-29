package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;

import java.util.UUID;

public interface ReplicationForwardingService {



    void forwardTimestampToForwardingQueue(TupleReplicationTimestampDto dto,
                                           DatabaseDto database);

    String extractSourceSiteId(String value);

    String extractSiteIdFromUrl(String replicaUrl);
}

