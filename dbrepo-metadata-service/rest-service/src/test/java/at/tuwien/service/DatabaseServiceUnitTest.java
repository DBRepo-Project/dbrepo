package at.tuwien.service;

import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.repository.DatabaseRepository;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseServiceUnitTest extends AbstractUnitTest {

    @MockBean
    private SearchServiceGateway searchServiceGateway;

    @MockBean
    private DataServiceGateway dataServiceGateway;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private DatabaseService databaseService;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

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
    public void findAllPublicByInternalName_notFound_fails() {

        /* mock */
        when(databaseRepository.findAllPublicByInternalNameDesc(DATABASE_1_INTERNALNAME))
                .thenReturn(List.of());

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.findAllPublicByInternalName(DATABASE_1_INTERNALNAME);
        });
    }

    @Test
    public void updatePassword_succeeds() throws DataServiceException, DatabaseNotFoundException,
            DataServiceConnectionException {

        /* mock */
        when(databaseRepository.findAllPublicOrReadAccessDesc(USER_1_ID))
                .thenReturn(List.of(DATABASE_1));
        doNothing()
                .when(dataServiceGateway)
                .updateDatabase(eq(DATABASE_1_ID), any(UpdateUserPasswordDto.class));

        /* test */
        databaseService.updatePassword(DATABASE_1, USER_1);
    }

    @Test
    public void modifyImage_succeeds() throws SearchServiceException, DatabaseNotFoundException, SearchServiceConnectionException {
        final byte[] image = new byte[]{1, 2, 3, 4, 5};

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

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
                .thenReturn(DATABASE_1_DTO);

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
            QueryNotFoundException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException, ViewNotFoundException {

        /* mock */
        when(dataServiceGateway.getViewSchemas(DATABASE_1_ID))
                .thenReturn(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO, VIEW_4_DTO)); /* <<< */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final Database response = databaseService.updateViewMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateViewMetadata_allKnown_succeeds() throws SearchServiceException, DataServiceException,
            QueryNotFoundException, DatabaseNotFoundException, SearchServiceConnectionException,
            DataServiceConnectionException, ViewNotFoundException {

        /* mock */
        when(dataServiceGateway.getViewSchemas(DATABASE_1_ID))
                .thenReturn(List.of(VIEW_1_DTO, VIEW_2_DTO, VIEW_3_DTO)); /* <<< */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final Database response = databaseService.updateViewMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateTableMetadata_empty_succeeds() throws TableNotFoundException, SearchServiceException,
            MalformedException, DataServiceException, QueryNotFoundException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableSchemas(DATABASE_1_ID))
                .thenReturn(List.of());
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final Database response = databaseService.updateTableMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateTableMetadata_allKnown_succeeds() throws TableNotFoundException, SearchServiceException,
            MalformedException, DataServiceException, QueryNotFoundException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableSchemas(DATABASE_1_ID))
                .thenReturn(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final Database response = databaseService.updateTableMetadata(DATABASE_1);
        assertNotNull(response);
    }

    @Test
    public void updateTableMetadata_oneMissing_succeeds() throws TableNotFoundException, SearchServiceException,
            MalformedException, DataServiceException, QueryNotFoundException, DatabaseNotFoundException,
            SearchServiceConnectionException, DataServiceConnectionException {

        /* mock */
        when(dataServiceGateway.getTableSchemas(DATABASE_1_ID))
                .thenReturn(List.of(TABLE_1_DTO, TABLE_2_DTO, TABLE_3_DTO, TABLE_4_DTO, TABLE_5_DTO));
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        final Database response = databaseService.updateTableMetadata(DATABASE_1);
        assertNotNull(response);
        final Optional<Table> optional = response.getTables()
                .stream()
                .filter(t -> t.getInternalName().equals(TABLE_5_INTERNALNAME))
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
        when(databaseRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.findById(9999L);
        });
    }

    @Test
    public void create_succeeds() throws Exception {

        /* mock */
        when(dataServiceGateway.createDatabase(any(CreateDatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);

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

        /* test */
        generic_modifyVisibility(DATABASE_1, true, true);
    }

    @Test
    public void visibility_searchServiceError_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(SearchServiceException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            generic_modifyVisibility(DATABASE_1, true, true);
        });
    }

    @Test
    public void visibility_searchServiceNotFound_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_modifyVisibility(DATABASE_1, true, true);
        });
    }

    @Test
    public void visibility_searchServiceConnection_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            generic_modifyVisibility(DATABASE_1, true, true);
        });
    }

    @Test
    public void modifyOwner_succeeds() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* test */
        final Database response = generic_modifyOwner(DATABASE_1, USER_2);
        assertEquals(USER_2, response.getOwner());
        assertEquals(USER_2_ID, response.getOwnedBy());
        assertEquals(USER_2, response.getContact());
        assertEquals(USER_2_ID, response.getContactPerson());
    }

    @Test
    public void modifyOwner_searchServiceError_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(SearchServiceException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(SearchServiceException.class, () -> {
            generic_modifyOwner(DATABASE_1, USER_2);
        });
    }

    @Test
    public void modifyOwner_searchServiceNotFound_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            generic_modifyOwner(DATABASE_1, USER_2);
        });
    }

    @Test
    public void modifyOwner_searchServiceConnection_fails() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* mock */
        doThrow(SearchServiceConnectionException.class)
                .when(searchServiceGateway)
                .update(DATABASE_1);

        /* test */
        assertThrows(SearchServiceConnectionException.class, () -> {
            generic_modifyOwner(DATABASE_1, USER_2);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected Database generic_create() throws DataServiceException,
            DataServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            ContainerNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(DATABASE_1);

        /* test */
        final Database response = databaseService.create(CONTAINER_1, DATABASE_1_CREATE, USER_1);
        assertTrue(response.getInternalName().startsWith(DATABASE_1_INTERNALNAME));
        assertNotNull(response.getContainer());
        assertNotNull(response.getTables());
        assertNotNull(response.getViews());
        assertNotNull(response.getAccesses());
        assertNotNull(response.getIdentifiers());
        assertNotNull(response.getOwnedBy());
        assertNotNull(response.getOwner());
        assertNotNull(response.getContactPerson());
        assertNotNull(response.getContact());
        assertNotNull(response.getImage());
        assertNotNull(response.getExchangeName());
        return response;
    }

    protected Database generic_modifyOwner(Database database, User newOwner) throws DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);

        /* test */
        final Database response = databaseService.modifyOwner(database, newOwner);
        assertNotNull(response);
        return response;
    }

    protected Database generic_modifyVisibility(Database database, Boolean isPublic, Boolean isSchemaPublic)
            throws DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);

        /* test */
        final Database response = databaseService.modifyVisibility(database, DatabaseModifyVisibilityDto.builder()
                .isPublic(isPublic)
                .isSchemaPublic(isSchemaPublic)
                .build());
        assertNotNull(response);
        return response;
    }

}
