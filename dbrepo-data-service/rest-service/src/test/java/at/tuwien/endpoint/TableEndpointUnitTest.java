package at.tuwien.endpoint;

import at.tuwien.ExportResourceDto;
import at.tuwien.api.database.query.ImportCsvDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.*;
import at.tuwien.endpoints.TableEndpoint;
import at.tuwien.exception.*;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.TableService;
import at.tuwien.test.AbstractUnitTest;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;

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
    private MetadataServiceGateway metadataServiceGateway;

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
    @WithAnonymousUser
    public void getData_succeeds() throws DatabaseUnavailableException, TableNotFoundException, TableMalformedException,
            SQLException, QueryMalformedException, RemoteUnavailableException, PaginationException, MetadataServiceException,
            NotAllowedException {

        /* mock */
        when(metadataServiceGateway.getTableById(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(TABLE_8_PRIVILEGED_DTO);
        when(tableService.getCount(eq(TABLE_8_PRIVILEGED_DTO), any(Instant.class)))
                .thenReturn(TABLE_8_DATA_COUNT);
        when(tableService.getData(eq(TABLE_8_PRIVILEGED_DTO), any(Instant.class), eq(0L), eq(10L)))
                .thenReturn(TABLE_8_DATA_DTO);

        /* test */
        final ResponseEntity<QueryResultDto> response = tableEndpoint.getData(DATABASE_3_ID, TABLE_8_ID, null, null, null, httpServletRequest, null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getHeaders().get("Access-Control-Expose-Headers"));
        assertEquals(1, response.getHeaders().get("Access-Control-Expose-Headers").size());
        assertEquals("X-Count", response.getHeaders().get("Access-Control-Expose-Headers").get(0));

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
    public void createTuple_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
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
    public void createTuple_noRole_fails() {
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
    public void createTuple_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
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
    public void createTuple_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
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
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void createTuple_writeOwnAccess_succeeds() throws TableNotFoundException, RemoteUnavailableException,
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
    public void createTuple_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
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
    public void createTuple_writeAllAccessForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
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
            StorageUnavailableException, QueryMalformedException, SidecarExportException, RemoteUnavailableException,
            StorageNotFoundException, SQLException, MetadataServiceException {
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
        final ResponseEntity<InputStreamResource> response = tableEndpoint.exportData(DATABASE_3_ID, TABLE_8_ID, null, null);
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
            tableEndpoint.exportData(DATABASE_1_ID, TABLE_1_ID, null, null);
        });

    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"insert-table-data"})
    public void importData_succeeds() throws DatabaseUnavailableException, TableNotFoundException,
            SidecarImportException, NotAllowedException, QueryMalformedException, RemoteUnavailableException,
            StorageNotFoundException, SQLException, MetadataServiceException {
        final ImportCsvDto request = ImportCsvDto.builder()
                .skipLines(1L)
                .lineTermination("\\n")
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
    public void importData_noRole_fails() {
        final ImportCsvDto request = ImportCsvDto.builder()
                .skipLines(1L)
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
    public void importData_tableNotFound_fails() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final ImportCsvDto request = ImportCsvDto.builder()
                .skipLines(1L)
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
    public void importData_readAccess_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final ImportCsvDto request = ImportCsvDto.builder()
                .skipLines(1L)
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
    public void importData_writeOwnAccess_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, SidecarImportException, QueryMalformedException,
            StorageNotFoundException, MetadataServiceException {
        final ImportCsvDto request = ImportCsvDto.builder()
                .skipLines(1L)
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
    public void importData_writeOwnAccessForeign_fails() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, MetadataServiceException {
        final ImportCsvDto request = ImportCsvDto.builder()
                .skipLines(1L)
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
    public void importData_writeAllAccessForeign_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            NotAllowedException, DatabaseUnavailableException, SidecarImportException, QueryMalformedException,
            StorageNotFoundException, MetadataServiceException {
        final ImportCsvDto request = ImportCsvDto.builder()
                .skipLines(1L)
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

}
