package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.DataReplicationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.DatabaseNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.TableNotificationDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ViewNotificationDto;
import org.springframework.http.HttpMethod;

public interface ReplicationService {

    int replicateDatabase(DatabaseNotificationDto notification);

    int replicateTable(TableNotificationDto notification);

    int replicateView(ViewNotificationDto notification);

    int replicateData(DataReplicationDto request, HttpMethod method);
}
