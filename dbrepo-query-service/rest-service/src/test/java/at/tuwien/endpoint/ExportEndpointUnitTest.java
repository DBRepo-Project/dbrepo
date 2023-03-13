package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ExportEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

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

    @Autowired
    private ExportEndpoint exportEndpoint;

    @Test
    public void export_publicAnonymous_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, null, null, null, "text/csv");
    }

    @Test
    public void export_publicResearcherRead_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_3_RESEARCHER_READ_ACCESS, "text/csv");
    }

    @Test
    public void export_publicResearcherWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_3_RESEARCHER_WRITE_OWN_ACCESS, "text/csv");
    }

    @Test
    public void export_publicResearcherWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_3_RESEARCHER_WRITE_ALL_ACCESS, "text/csv");
    }

    @Test
    public void export_publicDeveloperRead_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_3_DEVELOPER_READ_ACCESS, "text/csv");
    }

    @Test
    public void export_publicDeveloperWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_3_DEVELOPER_WRITE_OWN_ACCESS, "text/csv");
    }

    @Test
    public void export_publicDeveloperWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_3_DEVELOPER_WRITE_ALL_ACCESS, "text/csv");
    }

    @Test
    public void export_publicDataStewardRead_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_3_PRINCIPAL, USER_3_USERNAME, DATABASE_3_DATA_STEWARD_READ_ACCESS, "text/csv");
    }

    @Test
    public void export_publicDataStewardWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_3_PRINCIPAL, USER_3_USERNAME, DATABASE_3_DATA_STEWARD_WRITE_OWN_ACCESS, "text/csv");
    }

    @Test
    public void export_publicDataStewardWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, null, USER_3_PRINCIPAL, USER_3_USERNAME, DATABASE_3_DATA_STEWARD_WRITE_ALL_ACCESS, "text/csv");
    }

    @Test
    public void export_publicResearcherReadWithTimestamp_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {
        final Instant timestamp = Instant.now();

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, timestamp, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_3_RESEARCHER_READ_ACCESS, "text/csv");
    }

    @Test
    public void export_publicResearcherReadWithTimestampInFuture_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {
        final Instant timestamp = Instant.now().plus(10, ChronoUnit.DAYS);

        /* test */
        export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, timestamp, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_3_RESEARCHER_READ_ACCESS, "text/csv");
    }

    @Test
    public void export_publicResearcherReadWithTimestampInFutureInvalidHeader_fails() {
        final Instant timestamp = Instant.now().plus(10, ChronoUnit.DAYS);

        /* test */
        assertThrows(HeaderInvalidException.class, () -> {
            export_generic(CONTAINER_3_ID, DATABASE_3_ID, TABLE_8_ID, DATABASE_3, timestamp, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_3_RESEARCHER_READ_ACCESS, "text/xml");
        });
    }

    /* ################################################################################################### */
    /* ## PRIVATE DATABASES                                                                             ## */
    /* ################################################################################################### */

    @Test
    public void export_privateAnonymous_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, null, null, null, "text/csv");
        });
    }

    @Test
    public void export_privateResearcherRead_succeeds() throws UserNotFoundException, TableMalformedException,
            NotAllowedException, IOException, FileStorageException, PaginationException, TableNotFoundException,
            DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException,
            ContainerNotFoundException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_2_RESEARCHER_READ_ACCESS, "text/csv");
    }

    @Test
    public void export_privateResearcherWriteOwn_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_2_RESEARCHER_WRITE_OWN_ACCESS, "text/csv");
    }

    @Test
    public void export_privateResearcherWriteAll_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_2_RESEARCHER_WRITE_ALL_ACCESS, "text/csv");
    }

    @Test
    public void export_privateDeveloperRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_2_DEVELOPER_READ_ACCESS, "text/csv");
        });
    }

    @Test
    public void export_privateDeveloperWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_2_DEVELOPER_WRITE_OWN_ACCESS, "text/csv");
        });
    }

    @Test
    public void export_privateDeveloperWriteAll_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_2_PRINCIPAL, USER_2_USERNAME, DATABASE_2_DEVELOPER_WRITE_ALL_ACCESS, "text/csv");
        });
    }

    @Test
    public void export_privateDataStewardRead_succeeds() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_3_PRINCIPAL, USER_3_USERNAME, DATABASE_2_DATA_STEWARD_READ_ACCESS, "text/csv");
        });
    }

    @Test
    public void export_privateDataStewardWriteOwn_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_3_PRINCIPAL, USER_3_USERNAME, DATABASE_2_DATA_STEWARD_WRITE_OWN_ACCESS, "text/csv");
        });
    }

    @Test
    public void export_privateDataStewardWriteAll_fails() {

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, null, USER_3_PRINCIPAL, USER_3_USERNAME, DATABASE_2_DATA_STEWARD_WRITE_ALL_ACCESS, "text/csv");
        });
    }

    @Test
    public void export_privateResearcherReadWithTimestamp_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {
        final Instant timestamp = Instant.now();

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, timestamp, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_2_RESEARCHER_READ_ACCESS, "text/csv");
    }

    @Test
    public void export_privateResearcherReadWithTimestampInFuture_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException,
            UserNotFoundException, IOException, HeaderInvalidException {
        final Instant timestamp = Instant.now().plus(10, ChronoUnit.DAYS);

        /* test */
        export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, timestamp, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_2_RESEARCHER_READ_ACCESS, "text/csv");
    }

    @Test
    public void export_privateResearcherReadWithTimestampInFutureInvalidHeader_fails() {
        final Instant timestamp = Instant.now().plus(10, ChronoUnit.DAYS);

        /* test */
        assertThrows(HeaderInvalidException.class, () -> {
            export_generic(CONTAINER_2_ID, DATABASE_2_ID, TABLE_4_ID, DATABASE_2, timestamp, USER_1_PRINCIPAL, USER_1_USERNAME, DATABASE_2_RESEARCHER_READ_ACCESS, "text/xml");
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void export_generic(Long containerId, Long databaseId, Long tableId, Database database, Instant timestamp,
                                  Principal principal, String username, DatabaseAccess access, String accept) throws IOException,
            DatabaseNotFoundException, UserNotFoundException, TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, QueryMalformedException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, HeaderInvalidException {
        final ExportResource resource = ExportResource.builder()
                .filename("location.csv")
                .resource(new InputStreamResource(FileUtils.openInputStream(new File("../../dbrepo-metadata-db/test/src/test/resources/weather/location.csv"))))
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
        when(tableRepository.find(containerId, databaseId, tableId))
                .thenReturn(Optional.of(TABLE_1));
        when(queryService.tableFindAll(containerId, databaseId, tableId, timestamp, principal))
                .thenReturn(resource);

        /* test */
        final ResponseEntity<InputStreamResource> response = exportEndpoint.export(containerId, databaseId, tableId,
                timestamp, principal, accept);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
