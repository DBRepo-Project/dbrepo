package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicaLocation;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.View;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.metadata.entity.ReplicationNotificationOutbox;

import java.util.List;
import java.util.UUID;

public interface ReplicationService {

    UUID reserveCreation(String kind, UUID parentId, String origin, UUID creationId,
                         String physicalName, Object payload);

    <T> T findCreated(Class<T> type, UUID id);

    void replicateDatabase(CreateDatabaseDto createDatabaseDto, UUID creationId, UserDto owner);

    void replicateTable(CreateTableDto createTableDto, UUID databaseId, List<ReplicaLocation> replicas,
                        UUID creationId);

    void replicateTableDelete(Database database, Table table);

    void replicateView(View view);

    List<ReplicationNotificationOutbox> findOutboxEntries();

    int retryDueOutboxEntries();

    boolean retryOutboxEntry(UUID id);

}
