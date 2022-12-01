package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.SortType;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.query.QueryTypeDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.service.QueryService;
import at.tuwien.service.StoreService;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class QueryEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private ImageRepository imageRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private QueryService queryService;

    @MockBean
    private StoreService storeService;

    @Autowired
    private QueryEndpoint queryEndpoint;

    @Test
    public void execute_publicAnonymized_fails() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.empty());
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request, page, size, principal, sortDirection,
                    sortColumn);
        });
    }

    @Test
    public void execute_publicRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_1_ACCESS));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void execute_publicWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_2_ACCESS));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void execute_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_2_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_3_ACCESS));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void execute_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, SortException, NotAllowedException,
            PaginationException {
        final ExecuteStatementDto request = ExecuteStatementDto.builder()
                .statement(QUERY_1_STATEMENT)
                .build();
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(Optional.of(DATABASE_3_ACCESS));
        when(queryService.execute(CONTAINER_1_ID, DATABASE_1_ID, request, QueryTypeDto.QUERY,
                principal, page, size, sortDirection, sortColumn))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.execute(CONTAINER_1_ID, DATABASE_1_ID, request,
                page, size, principal, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void reExecute_publicAnonymized_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {
        final Principal principal = null;
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1, page, size, sortDirection, sortColumn, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                principal, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void reExecute_publicRead_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_2_DETAILS, USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_1_ACCESS));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1, page, size, sortDirection, sortColumn, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                principal, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void reExecute_publicWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_2_DETAILS, USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_2_ACCESS));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1, page, size, sortDirection, sortColumn, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                principal, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void reExecute_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_2_DETAILS, USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_3_ACCESS));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1, page, size, sortDirection, sortColumn, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                principal, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void reExecute_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException, TableMalformedException,
            DatabaseConnectionException, QueryMalformedException, ColumnParseException, DatabaseNotFoundException,
            ImageNotSupportedException, SortException, NotAllowedException,
            PaginationException, QueryNotFoundException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_1_DETAILS, USER_1_PASSWORD, USER_1_DETAILS.getAuthorities());
        final Long page = 0L;
        final Long size = 2L;
        final SortType sortDirection = SortType.ASC;
        final String sortColumn = "location";

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(Optional.of(DATABASE_3_ACCESS));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1, page, size, sortDirection, sortColumn, principal))
                .thenReturn(QUERY_1_RESULT_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = queryEndpoint.reExecute(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                principal, page, size, sortDirection, sortColumn);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(QUERY_1_RESULT_ID, response.getBody().getId());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResultNumber());
        assertEquals(QUERY_1_RESULT_NUMBER, response.getBody().getResult().size());
        assertEquals(QUERY_1_RESULT_RESULT, response.getBody().getResult());
    }

    @Test
    public void export_publicAnonymized_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {
        final Principal principal = null;
        final String accept = null;
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                accept, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void export_publicAnonymized_fails() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {
        final Principal principal = null;
        final String accept = "application/json";
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, accept,
                principal);
        assertEquals(HttpStatus.NOT_IMPLEMENTED, response.getStatusCode());
    }

    @Test
    public void export_publicRead_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_2_DETAILS, USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());
        final String accept = "text/csv";
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_1_ACCESS));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                accept, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void export_publicWriteOwn_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_2_DETAILS, USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());
        final String accept = "text/csv";
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_2_ACCESS));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                accept, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void export_publicWriteAll_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_2_DETAILS, USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());
        final String accept = "text/csv";
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_2_USERNAME))
                .thenReturn(Optional.of(DATABASE_3_ACCESS));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                accept, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    public void export_publicOwner_succeeds() throws UserNotFoundException, QueryStoreException,
            TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, NotAllowedException, QueryNotFoundException, FileStorageException,
            ContainerNotFoundException, IOException {
        final Principal principal = new UsernamePasswordAuthenticationToken(USER_1_DETAILS, USER_1_PASSWORD, USER_1_DETAILS.getAuthorities());
        final String accept = "text/csv";
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(Optional.of(DATABASE_2_ACCESS));
        when(storeService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(QUERY_1);
        when(queryService.findOne(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<?> response = queryEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID, QUERY_1_ID,
                accept, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

}
