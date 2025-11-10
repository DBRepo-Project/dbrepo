package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.TableEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import at.ac.tuwien.ifs.dbrepo.service.TableService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableEndpointUnitTest extends BaseTest {

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private SparkSession sparkSession;

    @MockitoBean
    private HttpServletRequest httpServletRequest;

    @MockitoBean
    private TableService tableService;

    @MockitoBean
    private SubsetService subsetService;

    @MockitoBean
    private DatabaseService databaseService;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    public static Stream<Arguments> size_arguments() {
        return Stream.of(
                Arguments.arguments("zero", 0L),
                Arguments.arguments("neg zero", -0L),
                Arguments.arguments("negative", -1L)
        );
    }

    public static Stream<Arguments> anyAccess_parameters() {
        return Stream.of(
                Arguments.arguments("read", AccessTypeDto.READ),
                Arguments.arguments("write_own", AccessTypeDto.WRITE_OWN),
                Arguments.arguments("write_all", AccessTypeDto.WRITE_ALL)
        );
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws DatabaseUnavailableException, TableMalformedException, TableExistsException,
            RemoteUnavailableException, SQLException, DatabaseNotFoundException, TableNotFoundException,
            QueryMalformedException, MetadataServiceException, ContainerNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(tableService.create(any(Database.class), any(CreateTableDto.class)))
                .thenReturn(TABLE_3_DTO);
        when(tableService.inspect(any(Database.class), anyString()))
                .thenReturn(TABLE_3_DTO);

        /* test */
        final ResponseEntity<TableDto> response = tableEndpoint.create(DATABASE_1_ID, TABLE_4_CREATE_DTO);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.create(DATABASE_1_ID, TABLE_4_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableEndpoint.create(DATABASE_1_ID, TABLE_4_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_unavailable_fails() throws TableMalformedException, DatabaseNotFoundException, SQLException,
            TableExistsException, RemoteUnavailableException, TableNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .create(DATABASE_1_CACHE, TABLE_4_CREATE_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.create(DATABASE_1_ID, TABLE_4_CREATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_missingPrimaryKey_fails() {
        final CreateTableDto request = CreateTableDto.builder()
                .name(TABLE_1_NAME)
                .description(TABLE_1_DESCRIPTION)
                .columns(TABLE_1_COLUMNS_CREATE_DTO)
                .constraints(TABLE_1_CONSTRAINTS_CREATE_INVALID_DTO) // <<<
                .build();

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            tableEndpoint.create(DATABASE_1_ID, request);
        });
    }

    @Test
    @WithAnonymousUser
    public void statistic_publicData_succeeds() throws DatabaseUnavailableException, TableNotFoundException, SQLException,
            TableMalformedException, RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            NotAllowedException {

        /* mock */
        when(metadataService.getTable(DATABASE_2_ID, TABLE_6_ID))
                .thenReturn(TABLE_6_CACHE);
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        when(tableService.getStatistics(any(Database.class), any(UUID.class), anyString()))
                .thenReturn(TABLE_6_STATISTIC_DTO);

        /* test */
        final ResponseEntity<TableStatisticDto> response = tableEndpoint.getStatistic(DATABASE_2_ID, TABLE_6_ID, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void statistic_privateDataNotAllowed_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getStatistic(DATABASE_3_ID, TABLE_8_ID, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void statistic_unavailable_fails() throws TableNotFoundException, TableMalformedException,
            RemoteUnavailableException, MetadataServiceException, SQLException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_2_ID, TABLE_7_ID))
                .thenReturn(TABLE_7_CACHE);
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .getStatistics(any(Database.class), any(UUID.class), anyString());

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.getStatistic(DATABASE_2_ID, TABLE_7_ID, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void statistic_notFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_1_ID, TABLE_1_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.getStatistic(DATABASE_1_ID, TABLE_1_ID, null);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_succeeds() throws RemoteUnavailableException, DatabaseUnavailableException,
            TableNotFoundException, QueryMalformedException, SQLException, MetadataServiceException,
            DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doNothing()
                .when(tableService)
                .delete(DATABASE_1_CACHE, TABLE_1_CACHE);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void delete_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_tableNotFound_fails() throws RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_1_ID, TABLE_1_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_unavailable_fails() throws RemoteUnavailableException, TableNotFoundException, SQLException,
            MetadataServiceException, QueryMalformedException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .delete(any(Database.class), any(Table.class));

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithAnonymousUser
    public void getData_publicDataPrivateSchema_succeeds() throws DatabaseUnavailableException, TableNotFoundException, QueryMalformedException,
            RemoteUnavailableException, PaginationException, MetadataServiceException, NotAllowedException,
            DatabaseNotFoundException, StorageUnavailableException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(metadataService.getTable(DATABASE_2_ID, TABLE_6_ID))
                .thenReturn(TABLE_6_CACHE);
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        when(subsetService.getData(any(Database.class), anyString()))
                .thenReturn(mock);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = tableEndpoint.getData(DATABASE_2_ID, TABLE_6_ID, null, null, null, "application/json", httpServletRequest, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

    }

    @Test
    @WithAnonymousUser
    public void getData_head_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            SQLException, QueryMalformedException, RemoteUnavailableException, PaginationException,
            MetadataServiceException, NotAllowedException, DatabaseNotFoundException, StorageUnavailableException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(metadataService.getTable(DATABASE_2_ID, TABLE_5_ID))
                .thenReturn(TABLE_5_CACHE);
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        when(tableService.getCount(any(Database.class), anyString(), any(Instant.class)))
                .thenReturn(3L);
        when(subsetService.getData(eq(DATABASE_2_CACHE), anyString()))
                .thenReturn(mock);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<?> response = tableEndpoint.getData(DATABASE_2_ID, TABLE_5_ID, null, null, null, "application/json", httpServletRequest, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Access-Control-Expose-Headers"));
        assertEquals("X-Count", response.getHeaders().get("Access-Control-Expose-Headers").get(0));
        assertNotNull(response.getHeaders().get("X-Count"));
        assertEquals("3", response.getHeaders().get("X-Count").get(0));

    }

    @Test
    @WithAnonymousUser
    public void getData_privateAnonymous_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, "application/json", httpServletRequest, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void getData_privateNoAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, "application/json", httpServletRequest, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void getData_notAllowed_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getData(DATABASE_3_ID, TABLE_8_ID, null, null, null, "application/json", httpServletRequest, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getData_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, QueryMalformedException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_2_ID, TABLE_5_ID))
                .thenReturn(TABLE_5_CACHE);
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        doThrow(QueryMalformedException.class)
                .when(subsetService)
                .getData(any(Database.class), anyString());
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.getData(DATABASE_2_ID, TABLE_5_ID, null, null, null, "application/json", httpServletRequest, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void getData_privateAccessUnavailable_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        doThrow(RemoteUnavailableException.class)
                .when(metadataService)
                .getDatabase(DATABASE_1_ID);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, "application/json", httpServletRequest, USER_2_PRINCIPAL);
        });
    }

    @ParameterizedTest
    @WithMockUser(username = USER_2_USERNAME)
    @MethodSource("anyAccess_parameters")
    public void getData_private_succeeds(String name, AccessTypeDto type) throws DatabaseUnavailableException,
            TableNotFoundException, QueryMalformedException, RemoteUnavailableException, PaginationException,
            MetadataServiceException, NotAllowedException, DatabaseNotFoundException, StorageUnavailableException, FormatNotAvailableException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(subsetService.getData(any(Database.class), anyString()))
                .thenReturn(mock);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, "application/json", httpServletRequest, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getData_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.getData(DATABASE_3_ID, TABLE_8_ID, null, null, null, "application/json", httpServletRequest, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            SQLException, StorageUnavailableException, StorageNotFoundException, MetadataServiceException, DatabaseNotFoundException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .createTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID, TOKEN_ACCESS_TOKEN);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void insertRawTuple_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, DatabaseNotFoundException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, TableMalformedException, StorageUnavailableException,
            SQLException, QueryMalformedException, StorageNotFoundException, DatabaseNotFoundException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .createTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_writeOwnAccess_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException, MetadataServiceException, DatabaseNotFoundException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, DatabaseNotFoundException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_writeAllAccessForeign_succeeds() throws TableNotFoundException,
            RemoteUnavailableException, NotAllowedException, DatabaseUnavailableException, TableMalformedException,
            QueryMalformedException, StorageUnavailableException, StorageNotFoundException, MetadataServiceException,
            DatabaseNotFoundException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            SQLException, MetadataServiceException, DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .updateTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID, TOKEN_ACCESS_TOKEN);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void updateTuple_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, DatabaseNotFoundException {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException, SQLException,
            MetadataServiceException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .updateTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_writeOwnAccess_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            SQLException, MetadataServiceException, DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .updateTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID, TOKEN_ACCESS_TOKEN);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void update_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, RemoteUnavailableException, SQLException, MetadataServiceException,
            DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .update(DATABASE_3_CACHE, TABLE_8_CACHE, TABLE_8_UPDATE_DTO);

        /* test */
        final ResponseEntity<TableDto> response = tableEndpoint.update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void update_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void update_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(RemoteUnavailableException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            tableEndpoint.update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void update_exception_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(MetadataServiceException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            tableEndpoint.update(DATABASE_3_ID, TABLE_8_ID, TABLE_8_UPDATE_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, DatabaseNotFoundException {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_writeAllAccessForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, TableMalformedException, QueryMalformedException,
            SQLException, MetadataServiceException, DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .updateTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID, TOKEN_ACCESS_TOKEN);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            SQLException, MetadataServiceException, DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .deleteTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID, TOKEN_ACCESS_TOKEN);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void deleteTuple_noRole_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException, SQLException,
            MetadataServiceException, TableMalformedException, QueryMalformedException, DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .deleteTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_writeOwnAccess_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, TableMalformedException, SQLException, QueryMalformedException,
            DatabaseUnavailableException, MetadataServiceException, DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .deleteTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID, TOKEN_ACCESS_TOKEN);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_writeAllAccessForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, TableMalformedException, QueryMalformedException,
            SQLException, MetadataServiceException, DatabaseNotFoundException, StorageUnavailableException, StorageNotFoundException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .deleteTuple(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID, TOKEN_ACCESS_TOKEN);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getHistory_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            RemoteUnavailableException, SQLException, NotAllowedException, MetadataServiceException, PaginationException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_2_ID, TABLE_5_ID))
                .thenReturn(TABLE_5_CACHE);
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        when(tableService.history(DATABASE_2_CACHE, TABLE_5_CACHE, null))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<TableHistoryDto>> response = tableEndpoint.getHistory(DATABASE_2_ID, TABLE_5_ID, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getHistory_privateNoRole_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getHistory(DATABASE_1_ID, TABLE_1_ID, null, null);
        });
    }

    @ParameterizedTest
    @MethodSource("size_arguments")
    @WithAnonymousUser
    public void getHistory_invalidSize_fails(String name, Long size) {

        /* test */
        assertThrows(PaginationException.class, () -> {
            tableEndpoint.getHistory(DATABASE_1_ID, TABLE_1_ID, size, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void getHistory_privateNoAccess_fails() throws NotAllowedException, RemoteUnavailableException,
            TableNotFoundException, MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getHistory(DATABASE_1_ID, TABLE_1_ID, null, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void getHistory_private_succeeds() throws NotAllowedException, RemoteUnavailableException, SQLException,
            TableNotFoundException, MetadataServiceException, DatabaseUnavailableException, PaginationException,
            DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(tableService.history(DATABASE_1_CACHE, TABLE_1_CACHE, 10L))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<TableHistoryDto>> response = tableEndpoint.getHistory(DATABASE_1_ID, TABLE_1_ID, null, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getHistory_unavailable_succeeds() throws RemoteUnavailableException, SQLException,
            TableNotFoundException, MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_2_ID, TABLE_5_ID))
                .thenReturn(TABLE_5_CACHE);
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .history(any(Database.class), any(Table.class), anyLong());

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.getHistory(DATABASE_2_ID, TABLE_5_ID, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getHistory_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.getHistory(DATABASE_3_ID, TABLE_8_ID, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getData_publicDataPrivateSchemaTextCsv_succeeds() throws TableNotFoundException, NotAllowedException,
            StorageUnavailableException, QueryMalformedException, RemoteUnavailableException, MetadataServiceException,
            DatabaseNotFoundException, DatabaseUnavailableException, FormatNotAvailableException, PaginationException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(metadataService.getTable(DATABASE_2_ID, TABLE_6_ID))
                .thenReturn(TABLE_6_CACHE);
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        when(subsetService.getData(any(Database.class), anyString()))
                .thenReturn(mock);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = tableEndpoint.getData(DATABASE_2_ID, TABLE_6_ID, null, null, null, "text/csv", httpServletRequest, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @ParameterizedTest
    @WithMockUser(username = USER_2_USERNAME)
    @MethodSource("anyAccess_parameters")
    public void getData_privateDataPrivateSchemaTextCsv_succeeds(String name, AccessTypeDto type)
            throws TableNotFoundException, NotAllowedException, StorageUnavailableException, QueryMalformedException,
            RemoteUnavailableException, MetadataServiceException, DatabaseNotFoundException,
            DatabaseUnavailableException, FormatNotAvailableException, PaginationException {
        final Dataset<Row> mock = sparkSession.emptyDataFrame();

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(subsetService.getData(any(Database.class), anyString()))
                .thenReturn(mock);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<?> response = tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, "text/csv", httpServletRequest, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void exportData_privateNoAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, "text/csv", httpServletRequest, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void findAll_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            RemoteUnavailableException, SQLException, MetadataServiceException, DatabaseNotFoundException,
            DatabaseMalformedException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        when(tableService.explore(DATABASE_3_CACHE))
                .thenReturn(List.of(TABLE_8_DTO));

        /* test */
        final ResponseEntity<List<TableDto>> response = tableEndpoint.findAll(DATABASE_3_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void findAll_anonymous_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            tableEndpoint.findAll(DATABASE_3_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void findAll_noRole_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            tableEndpoint.findAll(DATABASE_3_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void findAll_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException, SQLException,
            MetadataServiceException, DatabaseNotFoundException, DatabaseMalformedException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .explore(DATABASE_3_CACHE);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.findAll(DATABASE_3_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_succeeds() throws TableNotFoundException, NotAllowedException, RemoteUnavailableException,
            MetadataServiceException, StorageNotFoundException, MalformedException, StorageUnavailableException,
            DatabaseUnavailableException, QueryMalformedException, SQLException, TableMalformedException,
            DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination(null)
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doNothing()
                .when(tableService)
                .importDataset(DATABASE_3_CACHE, TABLE_8_CACHE, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID, TOKEN_ACCESS_TOKEN);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void importDataset_noRole_fails() {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_4_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataService)
                .getTable(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_unavailable_fails() throws RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, StorageNotFoundException, MalformedException, StorageUnavailableException,
            SQLException, QueryMalformedException, TableMalformedException, DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .importDataset(any(Database.class), any(Table.class), eq(request));

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_writeOwnAccess_fails() throws RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, StorageNotFoundException, MalformedException, StorageUnavailableException,
            SQLException, QueryMalformedException, TableMalformedException, DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        doThrow(SQLException.class)
                .when(tableService)
                .importDataset(any(Database.class), any(Table.class), eq(request));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_writeOwnAccess_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, StorageNotFoundException, MalformedException,
            StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException,
            DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_writeAllAccessForeign_succeeds() throws TableNotFoundException,
            RemoteUnavailableException, NotAllowedException, MetadataServiceException, StorageNotFoundException,
            MalformedException, StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException,
            DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_CACHE);
        when(metadataService.getDatabase(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);

        /* test */
        tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_privateWriteAllForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, StorageNotFoundException, MalformedException,
            StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException,
            DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.importDataset(DATABASE_1_ID, TABLE_1_ID, request, USER_3_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_privateWriteOwnForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_1_ID, TABLE_1_ID, request, USER_2_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_private_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, StorageNotFoundException, MalformedException,
            StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException,
            DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_2_ID))
                .thenReturn(TABLE_2_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        tableEndpoint.importDataset(DATABASE_1_ID, TABLE_2_ID, request, USER_2_PRINCIPAL, TOKEN_ACCESS_TOKEN);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_privateForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_1_ID, TABLE_1_ID, request, USER_2_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_privateReadAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, DatabaseNotFoundException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataService.getTable(DATABASE_1_ID, TABLE_2_ID))
                .thenReturn(TABLE_2_CACHE);
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_1_ID, TABLE_2_ID, request, USER_1_PRINCIPAL, TOKEN_ACCESS_TOKEN);
        });
    }

}
