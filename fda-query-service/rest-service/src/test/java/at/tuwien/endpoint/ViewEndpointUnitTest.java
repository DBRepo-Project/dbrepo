package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.repository.jpa.ViewRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.ViewService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private QueryService queryService;

    @MockBean
    private DatabaseService databaseService;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private TableRepository tableRepository;

    @MockBean
    private ViewService viewService;

    @Autowired
    private ViewEndpoint viewEndpoint;

    @Test
    public void findAll_publicAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, null, null);
    }

    @Test
    public void findAll_publicRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_READ_ACCESS);
    }

    @Test
    public void findAll_publicWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_OWN_ACCESS);
    }

    @Test
    public void findAll_publicWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void findAll_publicOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void create_publicAnonymous_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, null, null);
        });
    }

    @Test
    public void create_publicRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_READ_ACCESS);
        });
    }

    @Test
    public void create_publicWriteOwn_fails() throws UserNotFoundException, DatabaseConnectionException,
            ViewMalformedException, NotAllowedException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        create_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_OWN_ACCESS);
    }

    @Test
    public void create_publicWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException {

        /* test */
        create_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void create_publicOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException {

        /* test */
        create_generic(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void find_publicAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, null, null, null);
    }

    @Test
    public void find_publicRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_READ_ACCESS);
    }

    @Test
    public void find_publicWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_OWN_ACCESS);
    }

    @Test
    public void find_publicWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void find_publicOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void delete_publicAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, null, null, null);
        });
    }

    @Test
    public void delete_publicRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_READ_ACCESS);
        });
    }

    @Test
    public void delete_publicWriteOwn_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void delete_publicWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, ViewMalformedException,
            QueryMalformedException {

        /* test */
        delete_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void delete_publicOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, ViewMalformedException,
            QueryMalformedException {

        /* test */
        delete_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void data_publicAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, null, null, null);
    }

    @Test
    public void data_publicRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_READ_ACCESS);
    }

    @Test
    public void data_publicWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_OWN_ACCESS);
    }

    @Test
    public void data_publicWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    @Test
    public void data_publicOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_1_ID, DATABASE_1_ID, VIEW_1_ID, DATABASE_1, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_1_WRITE_ALL_ACCESS);
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    public void findAll_privateAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, null, null, null);
    }

    @Test
    public void findAll_privateRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_READ_ACCESS);
    }

    @Test
    public void findAll_privateWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_OWN_ACCESS);
    }

    @Test
    public void findAll_privateWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void findAll_privateOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException {

        /* test */
        findAll_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void create_privateAnonymous_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, null, null, null);
        });
    }

    @Test
    public void create_privateRead_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_READ_ACCESS);
        });
    }

    @Test
    public void create_privateWriteOwn_fails() throws UserNotFoundException, DatabaseConnectionException,
            ViewMalformedException, NotAllowedException, QueryMalformedException, DatabaseNotFoundException {

        /* test */
        create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_OWN_ACCESS);
    }

    @Test
    public void create_privateWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException {

        /* test */
        create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void create_privateOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException {

        /* test */
        create_generic(CONTAINER_2_ID, DATABASE_2_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void find_privateAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, null, null, null);
    }

    @Test
    public void find_privateRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_READ_ACCESS);
    }

    @Test
    public void find_privateWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_OWN_ACCESS);
    }

    @Test
    public void find_privateWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void find_privateOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException {

        /* test */
        find_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void delete_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, null, null, null);
        });
    }

    @Test
    public void delete_privateRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_READ_ACCESS);
        });
    }

    @Test
    public void delete_privateWriteOwn_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_OWN_ACCESS);
        });
    }

    @Test
    public void delete_privateWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, ViewMalformedException,
            QueryMalformedException {

        /* test */
        delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void delete_privateOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, ViewMalformedException,
            QueryMalformedException {

        /* test */
        delete_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void data_privateAnonymous_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, null, null, null);
    }

    @Test
    public void data_privateRead_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_READ_ACCESS);
    }

    @Test
    public void data_privateWriteOwn_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_OWN_ACCESS);
    }

    @Test
    public void data_privateWriteAll_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_2_USERNAME, USER_2_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }

    @Test
    public void data_privateOwner_succeeds() throws UserNotFoundException, NotAllowedException,
            DatabaseNotFoundException, ViewNotFoundException, DatabaseConnectionException, QueryMalformedException,
            QueryStoreException, TableMalformedException, ColumnParseException, ImageNotSupportedException,
            ContainerNotFoundException, PaginationException {

        /* test */
        data_generic(CONTAINER_2_ID, DATABASE_2_ID, VIEW_1_ID, DATABASE_2, USER_1_USERNAME, USER_1_PRINCIPAL, DATABASE_2_WRITE_ALL_ACCESS);
    }
    
    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void findAll_generic(Long containerId, Long databaseId, Database database, String username,
                                   Principal principal, DatabaseAccess access) throws UserNotFoundException,
            NotAllowedException, DatabaseNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
            when(viewService.findAll(databaseId, principal))
                    .thenReturn(List.of(VIEW_1));
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
            when(viewService.findAll(databaseId, principal))
                    .thenReturn(List.of(VIEW_1, VIEW_2));
        }

        /* test */
        final ResponseEntity<List<ViewBriefDto>> response = viewEndpoint.findAll(CONTAINER_1_ID, DATABASE_1_ID, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        if (access == null) {
            assertEquals(1, response.getBody().size());
        } else {
            assertEquals(2, response.getBody().size());
        }
    }

    protected void create_generic(Long containerId, Long databaseId, Database database, String username,
                                  Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, DatabaseConnectionException, ViewMalformedException, QueryMalformedException,
            NotAllowedException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(viewService.create(containerId, databaseId, request, principal))
                .thenReturn(VIEW_1);

        /* test */
        final ResponseEntity<ViewBriefDto> response = viewEndpoint.create(containerId, databaseId, request, principal);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(VIEW_1_ID, response.getBody().getId());
        assertEquals(VIEW_1_NAME, response.getBody().getName());
    }

    protected void find_generic(Long containerId, Long databaseId, Long viewId, Database database, String username,
                                Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(viewService.findById(databaseId, viewId, principal))
                .thenReturn(VIEW_1);

        /* test */
        final ResponseEntity<ViewDto> response = viewEndpoint.find(containerId, databaseId, viewId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(VIEW_1_ID, response.getBody().getId());
        assertEquals(VIEW_1_NAME, response.getBody().getName());
    }

    protected void delete_generic(Long containerId, Long databaseId, Long viewId, Database database, String username,
                                  Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException {

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        doNothing()
                .when(viewService)
                .delete(containerId, databaseId, viewId, principal);

        /* test */
        final ResponseEntity<?> response = viewEndpoint.delete(containerId, databaseId, viewId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    protected void data_generic(Long containerId, Long databaseId, Long viewId, Database database, String username,
                                Principal principal, DatabaseAccess access) throws DatabaseNotFoundException,
            UserNotFoundException, NotAllowedException, ViewNotFoundException, DatabaseConnectionException,
            QueryMalformedException, QueryStoreException, TableMalformedException, ColumnParseException,
            ImageNotSupportedException, ContainerNotFoundException, PaginationException {
        final ExecuteStatementDto statement = ExecuteStatementDto.builder()
                .statement(VIEW_1_QUERY)
                .build();
        final Long page = 0L;
        final Long size = 2L;

        /* mock */
        when(databaseService.find(containerId, databaseId))
                .thenReturn(database);
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        when(viewService.findById(databaseId, viewId, principal))
                .thenReturn(VIEW_1);
        when(queryService.execute(containerId, databaseId, statement, QueryTypeDto.VIEW, principal, page, size, null, null))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = viewEndpoint.data(containerId, databaseId, viewId, principal, page, size);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_DTO, response.getBody());
    }

}
