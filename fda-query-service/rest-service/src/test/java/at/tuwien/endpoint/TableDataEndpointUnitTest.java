package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.impl.QueryServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableDataEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @Autowired
    private TableDataEndpoint dataEndpoint;

    @MockBean
    private QueryServiceImpl queryService;

    @MockBean
    private DatabaseService databaseService;

    @Test
    @WithMockUser(username = USER_1_USERNAME, roles = "RESEARCHER")
    public void insert_succeeds() throws TableNotFoundException, TableMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, NotAllowedException, DatabaseConnectionException, QueryMalformedException {
        final ImportDto request = ImportDto.builder()
                .location("test:csv/csv_01.csv")
                .build();
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.importCsv(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request,
                principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void insert_locationNull_succeeds() throws TableNotFoundException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, ContainerNotFoundException,
            NotAllowedException, DatabaseConnectionException {
        final TableCsvDto request = TableCsvDto.builder()
                .data(Map.of("key", "value"))
                .build();
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        final ResponseEntity<?> response = dataEndpoint.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request,
                principal);
        assertNotNull(response);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void insert_locationAndDataNull_fails() throws DatabaseNotFoundException, TableNotFoundException, TableMalformedException, DatabaseConnectionException, ImageNotSupportedException, ContainerNotFoundException {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1, DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);
        doThrow(TableMalformedException.class).when(queryService)
                .insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, (TableCsvDto) null);

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            dataEndpoint.insert(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, null, principal);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getAll_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, PaginationException, ContainerNotFoundException,
            QueryStoreException, NotAllowedException, QueryMalformedException, SortException {
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, null, null, null, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_noPagination_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, PaginationException,
            ContainerNotFoundException, QueryStoreException, NotAllowedException, QueryMalformedException, SortException {
        final Long page = null;
        final Long size = null;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, DATABASE_1_CREATED, page, size, null, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_pageNull_fails() throws DatabaseNotFoundException {
        final Long page = null;
        final Long size = 1L;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(PaginationException.class, () -> {
            dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, DATABASE_1_CREATED, page, size, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_sizeNull_fails() throws DatabaseNotFoundException {
        final Long page = 1L;
        final Long size = null;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(PaginationException.class, () -> {
            dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, DATABASE_1_CREATED, page, size, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_negativePage_fails() throws DatabaseNotFoundException {
        final Long page = -1L;
        final Long size = 1L /* arbitrary */;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(PaginationException.class, () -> {
            dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, DATABASE_1_CREATED, page, size, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_sizeZero_fails() throws DatabaseNotFoundException {
        final Long page = 1L /* arbitrary */;
        final Long size = 0L;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(PaginationException.class, () -> {
            dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, DATABASE_1_CREATED, page, size, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void findAll_sizeNegative_fails() throws DatabaseNotFoundException {
        final Long page = 1L /* arbitrary */;
        final Long size = -1L;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(PaginationException.class, () -> {
            dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, DATABASE_1_CREATED, page, size, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getAll_parameter2_fails() throws DatabaseNotFoundException {
        final Long page = 1L;
        final Long size = 0L;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(PaginationException.class, () -> {
            dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, DATABASE_1_CREATED, page, size, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getAll_parameter_fails() throws DatabaseNotFoundException {
        final Long page = -1L;
        final Long size = 10L;
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertThrows(PaginationException.class, () -> {
            dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, principal, DATABASE_1_CREATED, page, size, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getAllTotal_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException, ContainerNotFoundException, QueryStoreException, NotAllowedException,
            QueryMalformedException, SortException {
        final Instant timestamp = Instant.now();
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        final ResponseEntity<QueryResultDto> response = dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID,
                TABLE_1_ID, principal, timestamp, null, null, null, null);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void getAllCount_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            PaginationException, ContainerNotFoundException, QueryStoreException, NotAllowedException,
            QueryMalformedException, SortException {
        final Instant timestamp = Instant.now();
        final Principal principal = SecurityContextHolder.getContext().getAuthentication();

        /* mock */
        doReturn(DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        final ResponseEntity<QueryResultDto> response = dataEndpoint.getAll(CONTAINER_1_ID, DATABASE_1_ID,
                TABLE_1_ID, principal, timestamp, null, null, null, null);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("FDA-COUNT"));
    }

}
