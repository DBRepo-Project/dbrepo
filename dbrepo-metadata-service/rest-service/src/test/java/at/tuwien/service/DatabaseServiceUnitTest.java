package at.tuwien.service;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.api.database.DatabaseModifyVisibilityDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.gateway.DataServiceGateway;
import at.tuwien.gateway.SearchServiceGateway;
import at.tuwien.repository.ContainerRepository;
import at.tuwien.repository.DatabaseRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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

    @MockBean
    private ContainerRepository containerRepository;

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
    public void create_notFound_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .cid(CONTAINER_1_ID)
                .name(DATABASE_1_NAME)
                .build();

        /* mock */
        when(containerRepository.findById(CONTAINER_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            databaseService.create(request, USER_1);
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
        when(containerRepository.findById(DATABASE_1.getCid()))
                .thenReturn(Optional.of(CONTAINER_1));
        when(dataServiceGateway.createDatabase(any(CreateDatabaseDto.class)))
                .thenReturn(DATABASE_1_DTO);

        /* test */
        generic_create(DATABASE_1_CREATE, DATABASE_1);
    }

    @Test
    public void create_containerNotFound_fails() {

        /* mock */
        when(containerRepository.findById(anyLong()))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            generic_create(DATABASE_1_CREATE, DATABASE_1);
        });
    }

    @Test
    public void create_dataServiceError_fails() throws ServiceException, ServiceConnectionException {

        /* mock */
        when(containerRepository.findById(DATABASE_1.getCid()))
                .thenReturn(Optional.of(CONTAINER_1));
        doThrow(ServiceException.class)
                .when(dataServiceGateway)
                .createDatabase(any(CreateDatabaseDto.class));

        /* test */
        assertThrows(ServiceException.class, () -> {
            generic_create(DATABASE_1_CREATE, DATABASE_1);
        });
    }

    @Test
    public void create_dataServiceConnection_fails() throws ServiceException, ServiceConnectionException {

        /* mock */
        when(containerRepository.findById(DATABASE_1.getCid()))
                .thenReturn(Optional.of(CONTAINER_1));
        doThrow(ServiceConnectionException.class)
                .when(dataServiceGateway)
                .createDatabase(any(CreateDatabaseDto.class));

        /* test */
        assertThrows(ServiceConnectionException.class, () -> {
            generic_create(DATABASE_1_CREATE, DATABASE_1);
        });
    }

    @Test
    public void visibility_succeeds() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* test */
        generic_modifyVisibility(DATABASE_1, true);
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
            generic_modifyVisibility(DATABASE_1, true);
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
            generic_modifyVisibility(DATABASE_1, true);
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
            generic_modifyVisibility(DATABASE_1, true);
        });
    }

    @Test
    public void modifyOwner_succeeds() throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {

        /* test */
        generic_modifyOwner(DATABASE_1, USER_1);
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

    protected Database generic_create(DatabaseCreateDto createDto, Database database) throws ServiceException,
            ServiceConnectionException, UserNotFoundException, DatabaseNotFoundException,
            ContainerNotFoundException, SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(searchServiceGateway.update(any(Database.class)))
                .thenReturn(DATABASE_1_DTO);
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);

        /* test */
        final Database response = databaseService.create(createDto, USER_1);
        assertEquals(database.getName(), response.getName());
        assertEquals(database.getIsPublic(), response.getIsPublic());
        assertTrue(response.getInternalName().startsWith(database.getInternalName()));
        assertNotNull(response.getContainer());
        assertNotNull(response.getTables());
        assertNotNull(response.getViews());
        assertNotNull(response.getAccesses());
        assertNotNull(response.getIdentifiers());
        assertNotNull(response.getCreatedBy());
        assertNotNull(response.getCreator());
        assertNotNull(response.getContactPerson());
        assertNotNull(response.getContact());
        assertNotNull(response.getCreatedBy());
        assertNotNull(response.getOwner());
        assertNull(response.getImage());
        assertNotNull(response.getExchangeName());
        assertEquals(database.getIsPublic(), response.getIsPublic());
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

    protected Database generic_modifyVisibility(Database database, Boolean isPublic) throws DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException {

        /* mock */
        when(databaseRepository.save(any(Database.class)))
                .thenReturn(database);

        /* test */
        final Database response = databaseService.modifyVisibility(database, DatabaseModifyVisibilityDto.builder()
                .isPublic(isPublic)
                .build());
        assertNotNull(response);
        return response;
    }

}
