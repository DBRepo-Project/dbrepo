package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.ReplicationAccessStatus;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DatabaseReplicationAccessServiceUnitTest {

    @Mock
    private RabbitConfig rabbitConfig;

    @Mock
    private MetadataMapper metadataMapper;

    @Mock
    private DatabaseRepository databaseRepository;

    @Mock
    private DataServiceGateway dataServiceGateway;

    @Mock
    private SearchServiceGateway searchServiceGateway;

    @Mock
    private DatabaseCacheRepository databaseCacheRepository;

    private DatabaseServiceImpl service;

    @BeforeEach
    public void beforeEach() {
        service = new DatabaseServiceImpl(rabbitConfig, metadataMapper, databaseRepository, dataServiceGateway,
                searchServiceGateway, databaseCacheRepository);
    }

    @Test
    public void modifyReplicationAccess_pendingStoresIdentityWithoutPublishingItToSearch() throws Exception {
        final Database database = database();
        final ReplicationOwnerDto owner = owner();
        when(databaseRepository.save(database)).thenReturn(database);

        final Database result = service.modifyReplicationAccess(
                database, owner, ReplicationAccessStatus.PENDING, null);

        assertEquals(ReplicationAccessStatus.PENDING, result.getReplicationAccessStatus());
        assertEquals(owner.getSubject(), result.getOriginOwnerSubject());
        assertEquals("replication", result.getOwnedBy());
        verify(databaseCacheRepository).deleteById(database.getId());
        verify(searchServiceGateway, never()).update(database);
    }

    @Test
    public void modifyReplicationAccess_mappedTransfersOwnerAndUpdatesSearch() throws Exception {
        final Database database = database();
        final ReplicationOwnerDto owner = owner();
        when(databaseRepository.save(database)).thenReturn(database);

        final Database result = service.modifyReplicationAccess(
                database, owner, ReplicationAccessStatus.MAPPED, "alice");

        assertEquals(ReplicationAccessStatus.MAPPED, result.getReplicationAccessStatus());
        assertEquals("alice", result.getReplicationLocalUsername());
        assertEquals("alice", result.getOwnedBy());
        assertEquals("alice", result.getContactPerson());
        verify(searchServiceGateway).update(database);
    }

    private Database database() {
        return Database.builder()
                .id(UUID.randomUUID())
                .name("replicated database")
                .ownedBy("replication")
                .contactPerson("replication")
                .build();
    }

    private ReplicationOwnerDto owner() {
        return ReplicationOwnerDto.builder()
                .siteUrl("https://origin.example")
                .issuer("https://identity.example/realms/dbrepo")
                .subject(UUID.randomUUID().toString())
                .username("remote-alice")
                .build();
    }
}
