package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.AccessType;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import at.ac.tuwien.ifs.dbrepo.service.impl.DatabaseServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.DatabaseAccess;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OwnerTransferGrantUnitTest extends BaseTest {
    private final DatabaseRepository repository = mock(DatabaseRepository.class);
    private final DataServiceGateway data = mock(DataServiceGateway.class);
    private final SearchServiceGateway search = mock(SearchServiceGateway.class);
    private final SqlAccessSyncService sync = mock(SqlAccessSyncService.class);
    private final DatabaseServiceImpl service = new DatabaseServiceImpl(mock(RabbitConfig.class),
            mock(MetadataMapper.class), repository, data, search, mock(DatabaseCacheRepository.class));

    @BeforeEach
    void grantsMatchAnOwnedDatabaseAndReadOnlyMember() {
        org.springframework.test.util.ReflectionTestUtils.setField(service, "sqlAccessSyncService", sync);
        DATABASE_1.getAccesses().clear();
        DATABASE_1.getAccesses().add(DatabaseAccess.builder().database(DATABASE_1).hdbid(DATABASE_1_ID)
                .username(USER_1_USERNAME).type(AccessType.WRITE_ALL).build());
        DATABASE_1.getAccesses().add(DATABASE_1_USER_2_READ_ACCESS);
    }

    private void saveReturnsArgument() {
        when(repository.save(any(Database.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void previouslyRevokedOwnerGetsFreshWriteAllGrant() throws Exception {
        DATABASE_1.getAccesses().removeIf(access -> USER_2_USERNAME.equals(access.getUsername()));
        saveReturnsArgument();
        service.modifyOwner(DATABASE_1, USER_2_USERNAME);
        verifyNoInteractions(data);
        verify(sync).afterCommit(DATABASE_1_ID);
        assertTrue(DATABASE_1.getAccesses().stream().filter(a -> USER_2_USERNAME.equals(a.getUsername()))
                .findFirst().orElseThrow().isSqlGrantPending());
        assertEquals(USER_2_USERNAME, DATABASE_1.getOwnedBy());
        assertEquals(USER_2_USERNAME, DATABASE_1.getContactPerson());
        assertEquals(AccessType.WRITE_ALL, DATABASE_1.getAccesses().stream()
                .filter(access -> USER_2_USERNAME.equals(access.getUsername())).findFirst().orElseThrow().getType());
        final int grants = DATABASE_1.getAccesses().size();
        service.modifyOwner(DATABASE_1, USER_1_USERNAME);
        assertEquals(USER_1_USERNAME, DATABASE_1.getOwnedBy());
        assertEquals(grants, DATABASE_1.getAccesses().size());
        verifyNoMoreInteractions(data);
    }

    @Test
    void existingReadGrantIsUpgradedWithoutReplacingOtherMemberships() throws Exception {
        saveReturnsArgument();
        final var previousOwnerGrant = DATABASE_1.getAccesses().stream()
                .filter(access -> USER_1_USERNAME.equals(access.getUsername())).findFirst().orElseThrow();
        service.modifyOwner(DATABASE_1, USER_2_USERNAME);
        verifyNoInteractions(data);
        assertTrue(DATABASE_1_USER_2_READ_ACCESS.isSqlGrantPending());
        assertTrue(DATABASE_1.getAccesses().contains(previousOwnerGrant));
        assertEquals(AccessType.WRITE_ALL, DATABASE_1_USER_2_READ_ACCESS.getType());
    }

    @Test
    void failedSearchDoesNotApplyOrDispatchSqlGrant() throws Exception {
        saveReturnsArgument();
        doThrow(new SearchServiceConnectionException("Unavailable")).when(search).update(any(Database.class));
        assertThrows(SearchServiceConnectionException.class, () -> service.modifyOwner(DATABASE_1, USER_2_USERNAME));
        verifyNoInteractions(data, sync);
    }

    @Test
    void unchangedOwnerDoesNotRewriteGrants() throws Exception {
        saveReturnsArgument();
        service.modifyOwner(DATABASE_1, USER_1_USERNAME);
        verifyNoInteractions(data);
        assertEquals(USER_1_USERNAME, DATABASE_1.getOwnedBy());
    }
}
