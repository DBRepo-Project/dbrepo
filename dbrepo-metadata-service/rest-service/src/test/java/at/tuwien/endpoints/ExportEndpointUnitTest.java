package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
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
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class ExportEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private QueryService queryService;

    @MockBean
    private DatabaseService databaseService;

    @Autowired
    private ExportEndpoint exportEndpoint;

    @Test
    @WithAnonymousUser
    public void export_anonymous_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"export-table-data"})
    public void export_publicHasRoleNoAccess_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, NotAllowedException, QueryMalformedException, UserNotFoundException, IOException {

        /* test */
        export_generic(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, null, USER_1_PRINCIPAL, USER_1_ID, null);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"export-table-data"})
    public void export_publicHasRoleReadAccess_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, NotAllowedException, QueryMalformedException, UserNotFoundException, IOException {

        /* test */
        export_generic(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, null, USER_1_PRINCIPAL, USER_1_ID, DATABASE_1_USER_1_READ_ACCESS);
    }

    @Test
    @WithAnonymousUser
    public void export_publicReadWithTimestamp_succeeds() {
        final Instant timestamp = Instant.now();

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, timestamp, null, null, null);
        });
    }

    @Test
    public void export_publicReadWithTimestampInFuture_succeeds() {
        final Instant timestamp = Instant.now().plus(10, ChronoUnit.DAYS);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(DATABASE_1_ID, TABLE_1_ID, DATABASE_1, timestamp, null, null, null);
        });
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    @WithAnonymousUser
    public void export_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(DATABASE_2_ID, TABLE_1_ID, DATABASE_2, null, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-table-data"})
    public void export_privateHasRoleNoAccess_fails() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, NotAllowedException, QueryMalformedException, UserNotFoundException, IOException {

        /* test */
        export_generic(DATABASE_2_ID, TABLE_1_ID, DATABASE_2, null, USER_2_PRINCIPAL, USER_2_ID, null);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-table-data"})
    public void export_HasRoleReadAccess_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, NotAllowedException, QueryMalformedException, UserNotFoundException, IOException {

        /* test */
        export_generic(DATABASE_2_ID, TABLE_1_ID, DATABASE_2, null, USER_2_PRINCIPAL, USER_2_ID, DATABASE_2_USER_1_WRITE_OWN_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-table-data"})
    public void export_privateReadWithTimestamp_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, NotAllowedException, QueryMalformedException, UserNotFoundException, IOException {
        final Instant timestamp = Instant.now();

        /* test */
        export_generic(DATABASE_2_ID, TABLE_1_ID, DATABASE_2, timestamp, USER_2_PRINCIPAL, USER_2_ID, DATABASE_2_USER_1_READ_ACCESS);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"export-table-data"})
    public void export_privateReadWithTimestampInFuture_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, NotAllowedException, QueryMalformedException, UserNotFoundException, IOException {
        final Instant timestamp = Instant.now().plus(10, ChronoUnit.DAYS);

        /* test */
        export_generic(DATABASE_2_ID, TABLE_1_ID, DATABASE_2, timestamp, USER_2_PRINCIPAL, USER_2_ID, DATABASE_2_USER_1_READ_ACCESS);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void export_generic(Long databaseId, Long tableId, Database database, Instant timestamp,
                                  Principal principal, UUID userId, DatabaseAccess access) throws IOException,
            DatabaseNotFoundException, UserNotFoundException, TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, QueryMalformedException, ImageNotSupportedException, FileStorageException,
            PaginationException, NotAllowedException {
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("src/test/resources/weather/location.csv"))))
                .build();

        /* mock */
        when(databaseService.find(databaseId))
                .thenReturn(database);
        when(queryService.tableFindAll(databaseId, tableId, timestamp, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<InputStreamResource> response = exportEndpoint.export(databaseId, tableId,
                timestamp, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
