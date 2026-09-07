package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import at.ac.tuwien.ifs.dbrepo.service.outbox.ReplicationOutboxEntry;
import org.springframework.http.HttpMethod;

import java.util.List;
import java.util.UUID;

public interface ReplicationService {

    int replicateDatabase(DatabaseNotificationDto notification);

    int replicateTable(TableNotificationDto notification);

    int replicateView(ViewNotificationDto notification);

    int replicateData(DataReplicationDto request, HttpMethod method);

    List<ReplicationOutboxEntry> findOutboxEntries();

    int retryDueOutboxEntries();

    boolean retryOutboxEntry(UUID id);
}
