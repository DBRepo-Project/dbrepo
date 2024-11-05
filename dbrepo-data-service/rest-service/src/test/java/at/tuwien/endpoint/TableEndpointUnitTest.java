package at.tuwien.endpoint;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.*;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.SchemaService;
import at.tuwien.service.TableService;
import at.tuwien.test.AbstractUnitTest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableEndpointUnitTest extends AbstractUnitTest {

    @Autowired
    private TableEndpoint tableEndpoint;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @MockBean
    private TableService tableService;

    @MockBean
    private SchemaService schemaService;

    @MockBean
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
                Arguments.arguments("read", DATABASE_1_USER_2_READ_ACCESS_DTO),
                Arguments.arguments("write_own", DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO),
                Arguments.arguments("write_all", DATABASE_1_USER_2_WRITE_ALL_ACCESS_DTO)
        );
    }

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws DatabaseUnavailableException, TableMalformedException,
            DatabaseNotFoundException, TableExistsException, RemoteUnavailableException, SQLException,
            TableNotFoundException, QueryMalformedException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(tableService.createTable(DATABASE_1_PRIVILEGED_DTO, TABLE_4_CREATE_INTERNAL_DTO))
                .thenReturn(TABLE_4_DTO);
        when(schemaService.inspectTable(DATABASE_1_PRIVILEGED_DTO, TABLE_4_INTERNALNAME))
                .thenReturn(TABLE_4_DTO);

        /* test */
        final ResponseEntity<TableDto> response = tableEndpoint.create(DATABASE_1_ID, TABLE_4_CREATE_INTERNAL_DTO);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.create(DATABASE_1_ID, TABLE_4_CREATE_INTERNAL_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_databaseNotFound_fails() throws DatabaseNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataServiceGateway)
                .getDatabaseById(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableEndpoint.create(DATABASE_1_ID, TABLE_4_CREATE_INTERNAL_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_unavailable_fails() throws TableMalformedException, DatabaseNotFoundException, SQLException,
            TableExistsException, RemoteUnavailableException, TableNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .createTable(DATABASE_1_PRIVILEGED_DTO, TABLE_4_CREATE_INTERNAL_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.create(DATABASE_1_ID, TABLE_4_CREATE_INTERNAL_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_missingPrimaryKey_fails() {

        /* test */
        assertThrows(TableMalformedException.class, () -> {
            tableEndpoint.create(DATABASE_1_ID, TABLE_1_CREATE_INTERNAL_INVALID_DTO);
        });
    }

    @Test
    @WithAnonymousUser
    public void statistic_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException,
            SQLException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(tableService.getStatistics(any(PrivilegedTableDto.class)))
                .thenReturn(TABLE_8_STATISTIC_DTO);

        /* test */
        final ResponseEntity<TableStatisticDto> response = tableEndpoint.statistic(DATABASE_3_ID, TABLE_8_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void statistic_unavailable_fails() throws TableNotFoundException, TableMalformedException,
            RemoteUnavailableException, MetadataServiceException, SQLException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .getStatistics(any(PrivilegedTableDto.class));

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.statistic(DATABASE_3_ID, TABLE_8_ID);
        });
    }

    @Test
    @WithAnonymousUser
    public void statistic_notFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataServiceGateway)
                .getTableById(DATABASE_1_ID, TABLE_1_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.statistic(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_succeeds() throws RemoteUnavailableException, DatabaseUnavailableException,
            TableNotFoundException, QueryMalformedException, SQLException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        doNothing()
                .when(tableService)
                .delete(TABLE_1_PRIVILEGED_DTO);

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
                .when(metadataServiceGateway)
                .getTableById(DATABASE_1_ID, TABLE_1_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void delete_unavailable_fails() throws RemoteUnavailableException, TableNotFoundException, SQLException,
            MetadataServiceException, QueryMalformedException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .delete(TABLE_1_PRIVILEGED_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.delete(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    @WithAnonymousUser
    public void getData_succeeds() throws DatabaseUnavailableException, TableNotFoundException, TableMalformedException,
            SQLException, QueryMalformedException, RemoteUnavailableException, PaginationException, MetadataServiceException,
            NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(tableService.getPaginatedData(eq(TABLE_8_PRIVILEGED_DTO), any(Instant.class), eq(0L), eq(10L)))
                .thenReturn(TABLE_8_DATA_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = tableEndpoint.getData(DATABASE_3_ID, TABLE_8_ID, null, null, null, httpServletRequest, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

    }

    @Test
    @WithAnonymousUser
    public void getData_head_succeeds() throws DatabaseUnavailableException, TableNotFoundException, TableMalformedException,
            SQLException, QueryMalformedException, RemoteUnavailableException, PaginationException, MetadataServiceException,
            NotAllowedException {
        final HttpServletRequest mock = mock(HttpServletRequest.class);

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(mock.getMethod())
                .thenReturn("HEAD");
        when(tableService.getCount(eq(TABLE_8_PRIVILEGED_DTO), any(Instant.class)))
                .thenReturn(3L);
        when(tableService.getPaginatedData(eq(TABLE_8_PRIVILEGED_DTO), any(Instant.class), eq(0L), eq(10L)))
                .thenReturn(TABLE_8_DATA_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = tableEndpoint.getData(DATABASE_3_ID, TABLE_8_ID, null, null, null, mock, null);
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
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, httpServletRequest, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void getData_privateNoAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        doThrow(NotAllowedException.class)
                .when(metadataServiceGateway)
                .getAccess(DATABASE_1_ID, USER_2_ID);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, httpServletRequest, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void getData_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, TableMalformedException, SQLException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .getPaginatedData(eq(TABLE_8_PRIVILEGED_DTO), any(Instant.class), eq(0L), eq(10L));

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.getData(DATABASE_3_ID, TABLE_8_ID, null, null, null, httpServletRequest, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void getData_privateAccessUnavailable_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException, NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        doThrow(RemoteUnavailableException.class)
                .when(metadataServiceGateway)
                .getAccess(DATABASE_1_ID, USER_2_ID);

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, httpServletRequest, USER_2_PRINCIPAL);
        });
    }

    @ParameterizedTest
    @WithMockUser(username = USER_2_USERNAME)
    @MethodSource("anyAccess_parameters")
    public void getData_private_succeeds(String name, DatabaseAccessDto access) throws DatabaseUnavailableException,
            TableNotFoundException, TableMalformedException, SQLException, QueryMalformedException,
            RemoteUnavailableException, PaginationException, MetadataServiceException, NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_2_ID))
                .thenReturn(access);
        when(tableService.getPaginatedData(eq(TABLE_1_PRIVILEGED_DTO), any(Instant.class), eq(0L), eq(10L)))
                .thenReturn(TABLE_1_DATA_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = tableEndpoint.getData(DATABASE_1_ID, TABLE_1_ID, null, null, null, httpServletRequest, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getData_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataServiceGateway)
                .getTableById(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.getData(DATABASE_3_ID, TABLE_8_ID, null, null, null, httpServletRequest, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            SQLException, StorageUnavailableException, StorageNotFoundException, MetadataServiceException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO);
        doNothing()
                .when(tableService)
                .createTuple(TABLE_8_PRIVILEGED_DTO, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void insertRawTuple_noRole_fails() {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataServiceGateway)
                .getTableById(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, TableMalformedException, StorageUnavailableException,
            SQLException, QueryMalformedException, StorageNotFoundException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .createTuple(TABLE_8_PRIVILEGED_DTO, request);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_writeOwnAccess_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException, MetadataServiceException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO);

        /* test */
        tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void insertRawTuple_writeAllAccessForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, TableMalformedException, QueryMalformedException,
            StorageUnavailableException, StorageNotFoundException, MetadataServiceException {
        final TupleDto request = TupleDto.builder()
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 7L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_ALL_ACCESS_DTO);

        /* test */
        tableEndpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            SQLException, MetadataServiceException {
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
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO);
        doNothing()
                .when(tableService)
                .updateTuple(TABLE_8_PRIVILEGED_DTO, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void updateTuple_noRole_fails() {
        final TupleUpdateDto request = TupleUpdateDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .data(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                    put(COLUMN_8_2_INTERNAL_NAME, 23.0);
                }})
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
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
        doThrow(TableNotFoundException.class)
                .when(metadataServiceGateway)
                .getTableById(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
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
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException, SQLException,
            NotAllowedException, MetadataServiceException, TableMalformedException, QueryMalformedException {
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
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_ALL_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .updateTuple(TABLE_8_PRIVILEGED_DTO, request);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_writeOwnAccess_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            SQLException, MetadataServiceException {
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
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO);
        doNothing()
                .when(tableService)
                .updateTuple(TABLE_8_PRIVILEGED_DTO, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
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
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void updateTuple_writeAllAccessForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, TableMalformedException, QueryMalformedException,
            SQLException, MetadataServiceException {
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
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_ALL_ACCESS_DTO);
        doNothing()
                .when(tableService)
                .updateTuple(TABLE_8_PRIVILEGED_DTO, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            TableMalformedException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            SQLException, MetadataServiceException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO);
        doNothing()
                .when(tableService)
                .deleteTuple(TABLE_8_PRIVILEGED_DTO, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void deleteTuple_noRole_fails() {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataServiceGateway)
                .getTableById(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException, SQLException,
            NotAllowedException, MetadataServiceException, TableMalformedException, QueryMalformedException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .deleteTuple(TABLE_8_PRIVILEGED_DTO, request);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_writeOwnAccess_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, TableMalformedException, SQLException, QueryMalformedException,
            DatabaseUnavailableException, MetadataServiceException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO);
        doNothing()
                .when(tableService)
                .deleteTuple(TABLE_8_PRIVILEGED_DTO, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"delete-table-data"})
    public void deleteTuple_writeAllAccessForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, TableMalformedException, QueryMalformedException,
            SQLException, MetadataServiceException {
        final TupleDeleteDto request = TupleDeleteDto.builder()
                .keys(new HashMap<>() {{
                    put(COLUMN_8_1_INTERNAL_NAME, 6L);
                }})
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_ALL_ACCESS_DTO);
        doNothing()
                .when(tableService)
                .deleteTuple(TABLE_8_PRIVILEGED_DTO, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getHistory_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            RemoteUnavailableException, SQLException, NotAllowedException, MetadataServiceException, PaginationException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(tableService.history(TABLE_8_PRIVILEGED_DTO, null))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<TableHistoryDto>> response = tableEndpoint.getHistory(DATABASE_3_ID, TABLE_8_ID, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getHistory_privateNoRole_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);

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
            TableNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        doThrow(NotAllowedException.class)
                .when(metadataServiceGateway)
                .getAccess(DATABASE_1_ID, USER_4_ID);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.getHistory(DATABASE_1_ID, TABLE_1_ID, null, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void getHistory_private_succeeds() throws NotAllowedException, RemoteUnavailableException, SQLException,
            TableNotFoundException, MetadataServiceException, DatabaseUnavailableException, PaginationException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_2_ID))
                .thenReturn(DATABASE_1_USER_2_READ_ACCESS_DTO);
        when(tableService.history(TABLE_1_PRIVILEGED_DTO, 10L))
                .thenReturn(List.of());

        /* test */
        final ResponseEntity<List<TableHistoryDto>> response = tableEndpoint.getHistory(DATABASE_1_ID, TABLE_1_ID, null, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getHistory_unavailable_succeeds() throws RemoteUnavailableException, SQLException,
            TableNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .history(TABLE_8_PRIVILEGED_DTO, 100L);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.getHistory(DATABASE_3_ID, TABLE_8_ID, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void getHistory_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        doThrow(TableNotFoundException.class)
                .when(metadataServiceGateway)
                .getTableById(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.getHistory(DATABASE_3_ID, TABLE_8_ID, null, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void exportData_succeeds() throws DatabaseUnavailableException, TableNotFoundException, NotAllowedException,
            StorageUnavailableException, QueryMalformedException, RemoteUnavailableException, StorageNotFoundException,
            SQLException, MetadataServiceException, MalformedException {
        final ExportResourceDto mock = ExportResourceDto.builder()
                .filename("deadbeef")
                .resource(new InputStreamResource(InputStream.nullInputStream()))
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(tableService.exportDataset(eq(TABLE_8_PRIVILEGED_DTO), any(Instant.class)))
                .thenReturn(mock);

        /* test */
        final ResponseEntity<InputStreamResource> response = tableEndpoint.exportDataset(DATABASE_3_ID, TABLE_8_ID, null, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());

    }

    @ParameterizedTest
    @WithMockUser(username = USER_2_USERNAME)
    @MethodSource("anyAccess_parameters")
    public void exportData_private_succeeds(String name, DatabaseAccessDto access) throws TableNotFoundException,
            NotAllowedException, StorageUnavailableException, QueryMalformedException, RemoteUnavailableException,
            MetadataServiceException, MalformedException {
        final ExportResourceDto mock = ExportResourceDto.builder()
                .filename("deadbeef")
                .resource(new InputStreamResource(InputStream.nullInputStream()))
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_2_ID))
                .thenReturn(access);
        when(tableService.exportDataset(eq(TABLE_1_PRIVILEGED_DTO), any(Instant.class)))
                .thenReturn(mock);

        /* test */
        final ResponseEntity<InputStreamResource> response = tableEndpoint.exportDataset(DATABASE_1_ID, TABLE_1_ID, null, USER_2_PRINCIPAL);
        assertEquals(HttpStatus.OK, response.getStatusCode());

    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void exportData_privateNoAccess_fails() throws TableNotFoundException, NotAllowedException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        doThrow(NotAllowedException.class)
                .when(metadataServiceGateway)
                .getAccess(DATABASE_1_ID, USER_4_ID);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.exportDataset(DATABASE_1_ID, TABLE_1_ID, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void getSchema_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            RemoteUnavailableException, SQLException, MetadataServiceException, DatabaseNotFoundException,
            DatabaseMalformedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(tableService.getSchemas(DATABASE_3_PRIVILEGED_DTO))
                .thenReturn(List.of(TABLE_8_DTO));

        /* test */
        final ResponseEntity<List<TableDto>> response = tableEndpoint.getSchema(DATABASE_3_ID);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithAnonymousUser
    public void getSchema_anonymous_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            tableEndpoint.getSchema(DATABASE_3_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void getSchema_noRole_succeeds() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            tableEndpoint.getSchema(DATABASE_3_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"system"})
    public void getSchema_unavailable_fails() throws TableNotFoundException, RemoteUnavailableException, SQLException,
            MetadataServiceException, DatabaseNotFoundException, DatabaseMalformedException {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .getSchemas(DATABASE_3_PRIVILEGED_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.getSchema(DATABASE_3_ID);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_succeeds() throws TableNotFoundException, NotAllowedException, RemoteUnavailableException,
            MetadataServiceException, StorageNotFoundException, MalformedException, StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException, SQLException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination(null)
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO);
        doNothing()
                .when(tableService)
                .importDataset(TABLE_8_PRIVILEGED_DTO, request);
        doNothing()
                .when(metadataServiceGateway)
                .updateTableStatistics(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        final ResponseEntity<Void> response = tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
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
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_4_PRINCIPAL);
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
                .when(metadataServiceGateway)
                .getTableById(DATABASE_3_ID, TABLE_8_ID);

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_unavailable_fails() throws RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, NotAllowedException, StorageNotFoundException, MalformedException,
            StorageUnavailableException, SQLException, QueryMalformedException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_ALL_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .importDataset(any(PrivilegedTableDto.class), eq(request));

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_writeOwnAccess_fails() throws RemoteUnavailableException, TableNotFoundException,
            MetadataServiceException, NotAllowedException, StorageNotFoundException, MalformedException,
            StorageUnavailableException, SQLException, QueryMalformedException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO);
        doThrow(SQLException.class)
                .when(tableService)
                .importDataset(any(PrivilegedTableDto.class), eq(request));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_READ_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_writeOwnAccess_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, StorageNotFoundException, MalformedException,
            StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_1_WRITE_OWN_ACCESS_DTO);

        /* test */
        tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_3_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_OWN_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_writeAllAccessForeign_succeeds() throws TableNotFoundException,
            RemoteUnavailableException, NotAllowedException, MetadataServiceException, StorageNotFoundException,
            MalformedException, StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_3_ID, USER_1_ID))
                .thenReturn(DATABASE_3_USER_3_WRITE_ALL_ACCESS_DTO);

        /* test */
        tableEndpoint.importDataset(DATABASE_3_ID, TABLE_8_ID, request, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_privateForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, StorageNotFoundException, MalformedException,
            StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_2_ID))
                .thenReturn(DATABASE_1_USER_2_WRITE_ALL_ACCESS_DTO);

        /* test */
        tableEndpoint.importDataset(DATABASE_1_ID, TABLE_1_ID, request, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_private_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException, StorageNotFoundException, MalformedException,
            StorageUnavailableException, DatabaseUnavailableException, QueryMalformedException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_2_ID))
                .thenReturn(TABLE_2_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_2_ID))
                .thenReturn(DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO);

        /* test */
        tableEndpoint.importDataset(DATABASE_1_ID, TABLE_2_ID, request, USER_2_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_privateForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(TABLE_1_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_2_ID))
                .thenReturn(DATABASE_1_USER_2_WRITE_OWN_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_1_ID, TABLE_1_ID, request, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"insert-table-data"})
    public void importDataset_privateReadAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final ImportDto request = ImportDto.builder()
                .header(true)
                .lineTermination("\\n")
                .location("deadbeef")
                .build();

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_2_ID))
                .thenReturn(TABLE_2_PRIVILEGED_DTO);
        when(metadataServiceGateway.getAccess(DATABASE_1_ID, USER_2_ID))
                .thenReturn(DATABASE_1_USER_2_READ_ACCESS_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            tableEndpoint.importDataset(DATABASE_1_ID, TABLE_2_ID, request, USER_2_PRINCIPAL);
        });
    }

}
