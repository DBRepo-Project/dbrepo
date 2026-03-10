package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.cache.DatabaseCacheRepository;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseModifyVisibilityDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.internal.UpdateUserPasswordDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.DataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.SearchServiceGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.DatabaseRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseServiceUnitTest extends BaseTest {

    @MockitoBean
    private SearchServiceGateway searchServiceGateway;

    @MockitoBean
    private DatabaseCacheRepository databaseCacheRepository;

    @MockitoBean
    private DataServiceGateway dataServiceGateway;

    @MockitoBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private DatabaseService databaseService;

    @Test
    public void findAll_succeeds() {
        /* mock */
        when(databaseRepository.findAllDesc())
                .thenReturn(List.of(DATABASE_1));

        /* test */
        final List<Database> response = databaseService.findAll();
        assertEquals(1, response.size());
        assertEquals(DATABASE_1, response.get(0));
    }

    @Test
    public void findById_succeeds() throws DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        final Database response = databaseService.findById(DATABASE_1_ID);

        /* test */
        assertEquals(DATABASE_1, response);
    }

    @Test
    public void findById_notFound_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.findById(DATABASE_1_ID);
        });
    }

    @Test
    public void modifyImage_succeeds() throws SearchServiceException, DatabaseNotFoundException, SearchServiceConnectionException {
        final byte[] image = new byte[]{1, 2, 3, 4, 5};

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);

        /* test */
        final Database response = databaseService.modifyImage(DATABASE_1, image);
        assertNotNull(response);
    }

    @Test
    public void modifyImage_searchServiceNotFound_fails() throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException {
        final byte[] image = new byte[]{1, 2, 3, 4, 5};

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(DatabaseNotFoundException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.modifyImage(DATABASE_1, image);
        });
    }

    @Test
    public void modifyImage_searchServiceConnection_fails() throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException {
        final byte[] image = new byte[]{1, 2, 3, 4, 5};

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            databaseService.modifyImage(DATABASE_1, image);
        });
    }

    @Test
    public void updateViewMetadata_empty_succeeds() throws SearchServiceException, DataServiceException,
            QueryNotFoundException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException, ViewNotFoundException {

        /* mock */
        when(dataServiceGateway.getViewSchemas(DATABASE_1_ID))
                .thenReturn(List.of());
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Database response = databaseService.updateViewMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateViewMetadata_searchServiceConnection_fails() throws SearchServiceException, DataServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, DataServiceConnectionException,
            ViewNotFoundException {

        /* mock */
        when(dataServiceGateway.getViewSchemas(DATABASE_1_ID))
                .thenReturn(List.of());
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            databaseService.updateViewMetadata(DATABASE_1);
        });
    }

    @Test
    public void updateViewMetadata_searchServiceNotFound_fails() throws SearchServiceException, DataServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, DataServiceConnectionException,
            ViewNotFoundException {

        /* mock */
        when(dataServiceGateway.getViewSchemas(DATABASE_1_ID))
                .thenReturn(List.of());
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        doThrow(DatabaseNotFoundException.class)
                .when(searchServiceGateway)
                .update(any(Database.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.updateViewMetadata(DATABASE_1);
        });
    }

    @Test
    public void updateViewMetadata_oneMissing_succeeds() throws SearchServiceException, DataServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, DataServiceConnectionException,
            ViewNotFoundException {

        /* mock */
        when(dataServiceGateway.getViewSchemas(DATABASE_1_ID))
                .thenReturn(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO, VIEW_4_DTO)); /* <<< */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Database response = databaseService.updateViewMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateViewMetadata_allKnown_succeeds() throws SearchServiceException, DataServiceException,
            DatabaseNotFoundException, SearchServiceConnectionException, DataServiceConnectionException,
            ViewNotFoundException {

        /* mock */
        when(dataServiceGateway.getViewSchemas(DATABASE_1_ID))
                .thenReturn(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO)); /* <<< */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Database response = databaseService.updateViewMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateTableMetadata_empty_succeeds() throws TableNotFoundException, SearchServiceException,
            MalformedException, DataServiceException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableSchemas(DATABASE_1_ID))
                .thenReturn(List.of());
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Database response = databaseService.updateTableMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateTableMetadata_allKnown_succeeds() throws TableNotFoundException, SearchServiceException,
            MalformedException, DataServiceException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableSchemas(DATABASE_1_ID))
                .thenReturn(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Database response = databaseService.updateTableMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateTableMetadata_oneMissing_succeeds() throws TableNotFoundException, SearchServiceException,
            MalformedException, DataServiceException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableSchemas(DATABASE_1_ID))
                .thenReturn(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO, TABLE_5_DTO));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);

        /* test */
        final Database response = databaseService.updateTableMetadata(DATABASE_1);
        assertNotNull(response);
        final Optional<Table> optional = response.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(TABLE_5_INTERNAL_NAME))
                .findFirst();
        assertTrue(optional.isPresent());
        final Table table = optional.get();
        table.getColumns()
                .forEach(column -> {
                    assertNotNull(column.getTable());
                    assertEquals(TABLE_5_ID, column.getTable().getId());
                });
        table.getConstraints()
                .getUniques()
                .forEach(uk -> {
                    assertNotNull(uk.getTable());
                    assertEquals(TABLE_5_ID, uk.getTable().getId());
                });
        table.getConstraints()
                .getForeignKeys()
                .forEach(fk -> {
                    assertNotNull(fk.getTable());
                    assertEquals(TABLE_5_ID, fk.getTable().getId());
                });
        table.getConstraints()
                .getPrimaryKey()
                .forEach(pk -> {
                    assertNotNull(pk.getTable());
                    assertEquals(TABLE_5_ID, pk.getTable().getId());
                    assertEquals(TABLE_5_COLUMNS.get(0), pk.getColumn());
                });
    }

    @Test
    public void find_succeeds() throws DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final Database response = databaseService.findById(DATABASE_1_ID);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void find_fails() {

        /* mock */
        when(databaseRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.findById(UUID.randomUUID());
        });
    }

    @Test
    public void create_succeeds() throws Exception {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doNothing()
                .when(dataServiceGateway)
                .createDatabase(any(CreateDatabaseDto.class));

        /* test */
        generic_create();
    }

    @Test
    public void create_dataServiceError_fails() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException {

        /* mock */
        doThrow(DataServiceException.class)
                .when(dataServiceGateway)
                .createDatabase(any(CreateDatabaseDto.class));

        /* test */
        assertThrows(DataServiceException.class, () -> {
            generic_create();
        });
    }

    @Test
    public void create_dataServiceConnection_fails() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException {

        /* mock */
        doThrow(DataServiceConnectionException.class)
                .when(dataServiceGateway)
                .createDatabase(any(CreateDatabaseDto.class));

        /* test */
        assertThrows(DataServiceConnectionException.class, () -> {
            generic_create();
        });
    }

    @Test
    public void visibility_succeeds() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);

        /* test */
        generic_modifyVisibility(DATABASE_1);
    }

    @Test
    public void visibility_searchServiceError_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(SearchServiceException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            generic_modifyVisibility(DATABASE_1);
        });
    }

    @Test
    public void visibility_searchServiceNotFound_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(DatabaseNotFoundException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_modifyVisibility(DATABASE_1);
        });
    }

    @Test
    public void visibility_searchServiceConnection_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            generic_modifyVisibility(DATABASE_1);
        });
    }

    @Test
    public void modifyOwner_succeeds() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        when(databaseCacheRepository.save(any(at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database.class)))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        final Database response = generic_modifyOwner(DATABASE_1, USER_2_USERNAME);
        assertEquals(USER_2_USERNAME, response.getOwnedBy());
        assertEquals(USER_2_USERNAME, response.getContactPerson());
    }

    @Test
    public void modifyOwner_searchServiceError_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(SearchServiceException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            generic_modifyOwner(DATABASE_1, USER_2_USERNAME);
        });
    }

    @Test
    public void modifyOwner_searchServiceNotFound_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(DatabaseNotFoundException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_modifyOwner(DATABASE_1, USER_2_USERNAME);
        });
    }

    @Test
    public void modifyOwner_searchServiceConnection_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doNothing()
                .when(databaseCacheRepository)
                .deleteById(DATABASE_1_ID);
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            generic_modifyOwner(DATABASE_1, USER_2_USERNAME);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected Database generic_create() throws DataServiceException, DataServiceConnectionException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException,
            DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_BRIEF_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        final Database response = databaseService.create(CONTAINER_1, DATABASE_1_CREATE, USER_1_DTO);
        assertTrue(response.getInternalName().startsWith(DATABASE_1_INTERNAL_NAME));
        assertNotNull(response.getContainer());
        assertNotNull(response.getTables());
        assertNotNull(response.getViews());
        assertNotNull(response.getAccesses());
        assertNotNull(response.getIdentifiers());
        assertNotNull(response.getOwnedBy());
        assertNotNull(response.getContactPerson());
        assertNotNull(response.getImage());
        assertNotNull(response.getExchangeName());
        return response;
    }

    protected Database generic_modifyOwner(Database database, String newOwner) throws DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);

        /* test */
        final Database response = databaseService.modifyOwner(database, newOwner);
        assertNotNull(response);
        return response;
    }

    protected Database generic_modifyVisibility(Database database)
            throws DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);

        /* test */
        final Database response = databaseService.modifyVisibility(database, DatabaseModifyVisibilityDto.builder()
                .isPublic(true)
                .isSchemaPublic(true)
                .isDashboardEnabled(true)
                .build());
        assertNotNull(response);
        return response;
    }

}
