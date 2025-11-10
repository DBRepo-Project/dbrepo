package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.internal.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.User;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.DatabaseEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import at.ac.tuwien.ifs.dbrepo.service.DatabaseService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseEndpointUnitTest extends BaseTest {

    @Autowired
    private DatabaseEndpoint databaseEndpoint;

    @MockitoBean
    private AccessService accessService;

    @MockitoBean
    private DatabaseService databaseService;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private AnalyseService analyseService;

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_succeeds() throws DatabaseUnavailableException, RemoteUnavailableException,
            QueryStoreCreateException, ContainerNotFoundException, DatabaseMalformedException,
            MetadataServiceException, SQLException, MalformedException {

        /* mock */
        when(metadataService.getContainer(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_CACHE);
        when(databaseService.create(CONTAINER_1_CACHE, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_CACHE);
        doNothing()
                .when(databaseService)
                .createQueryStore(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        doNothing()
                .when(accessService)
                .create(eq(DATABASE_1_CACHE), any(User.class), any(AccessTypeDto.class));

        /* test */
        final ResponseEntity<Void> response = databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void create_noRole_fails() throws RemoteUnavailableException, ContainerNotFoundException,
            SQLException, DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(metadataService.getContainer(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_CACHE);
        when(databaseService.create(CONTAINER_1_CACHE, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_CACHE);
        doNothing()
                .when(accessService)
                .create(eq(DATABASE_1_CACHE), any(User.class), any(AccessTypeDto.class));

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_readonlyPasswordHashed_fails() throws RemoteUnavailableException, ContainerNotFoundException,
            SQLException, QueryStoreCreateException, DatabaseMalformedException, MetadataServiceException {
        final CreateDatabaseDto request = CreateDatabaseDto.builder()
                .internalName(DATABASE_1_INTERNAL_NAME)
                .containerId(CONTAINER_1_ID)
                .username(USER_1_USERNAME)
                .password(USER_1_PASSWORD)
                .readonlyUsername(CONTAINER_1_READONLY_USERNAME)
                .readonlyPassword(CONTAINER_1_READONLY_HASHED_PASSWORD)
                .userId(USER_1_ID)
                .privilegedUsername(CONTAINER_1_PRIVILEGED_USERNAME)
                .privilegedPassword(CONTAINER_1_PRIVILEGED_PASSWORD)
                .build();

        /* mock */
        when(metadataService.getContainer(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_CACHE);
        when(databaseService.create(CONTAINER_1_CACHE, request))
                .thenReturn(DATABASE_1_CACHE);
        doNothing()
                .when(databaseService)
                .createQueryStore(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        doNothing()
                .when(accessService)
                .create(eq(DATABASE_1_CACHE), any(User.class), any(AccessTypeDto.class));

        /* test */
        assertThrows(MalformedException.class, () -> {
            databaseEndpoint.create(request);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_unavailable_fails() throws RemoteUnavailableException, ContainerNotFoundException,
            MetadataServiceException, SQLException, QueryStoreCreateException {

        /* mock */
        when(metadataService.getContainer(CONTAINER_1_ID))
                .thenReturn(CONTAINER_1_CACHE);
        doThrow(QueryStoreCreateException.class)
                .when(databaseService)
                .createQueryStore(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);

        /* test */
        assertThrows(QueryStoreCreateException.class, () -> {
            databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_containerNotFound_fails() throws RemoteUnavailableException, ContainerNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(ContainerNotFoundException.class)
                .when(metadataService)
                .getContainer(CONTAINER_1_ID);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void create_queryStore_fails() throws RemoteUnavailableException, ContainerNotFoundException, SQLException,
            DatabaseMalformedException, QueryStoreCreateException, MetadataServiceException {

        /* mock */
        doThrow(ContainerNotFoundException.class)
                .when(metadataService)
                .getContainer(CONTAINER_1_ID);
        when(databaseService.create(CONTAINER_1_CACHE, DATABASE_1_CREATE_INTERNAL))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(QueryStoreCreateException.class)
                .when(databaseService)
                .createQueryStore(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            databaseEndpoint.create(DATABASE_1_CREATE_INTERNAL);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_succeeds() throws DatabaseUnavailableException, RemoteUnavailableException,
            DatabaseMalformedException, DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        final ResponseEntity<Void> response = databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_unavailable_fails() throws RemoteUnavailableException, DatabaseMalformedException,
            DatabaseNotFoundException, MetadataServiceException, SQLException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(SQLException.class)
                .when(databaseService)
                .update(DATABASE_1_CACHE, USER_1_UPDATE_PASSWORD_DTO);

        /* test */
        assertThrows(DatabaseUnavailableException.class, () -> {
            databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME)
    public void update_noRole_fails() throws RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_databaseNotFound_fails() throws RemoteUnavailableException, DatabaseNotFoundException,
            MetadataServiceException {

        /* mock */
        doThrow(DatabaseNotFoundException.class)
                .when(metadataService)
                .getDatabase(DATABASE_1_ID);

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_LOCAL_ADMIN_USERNAME, authorities = {"system"})
    public void update_password_fails() throws RemoteUnavailableException, DatabaseNotFoundException, SQLException,
            DatabaseMalformedException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        doThrow(DatabaseMalformedException.class)
                .when(databaseService)
                .update(DATABASE_1_CACHE, USER_1_UPDATE_PASSWORD_DTO);

        /* test */
        assertThrows(DatabaseMalformedException.class, () -> {
            databaseEndpoint.update(DATABASE_1_ID, USER_1_UPDATE_PASSWORD_DTO);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"analyse-datatypes"})
    public void analyseDatatypes_succeeds() throws DatabaseUnavailableException, StorageNotFoundException,
            AnalyseDataTypesException, ImageInvalidException, RemoteUnavailableException, MetadataServiceException,
            DatabaseNotFoundException, ColumnNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(analyseService.determineDataTypes(any(Image.class), anyString()))
                .thenReturn(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO);

        /* test */
        final ResponseEntity<SchemaAnalysisResultDto> response = databaseEndpoint.analyseDatatypes(DATABASE_1_ID, "s3key");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final SchemaAnalysisResultDto body = response.getBody();
        assertNotNull(body);
        assertEquals(TABLE_1_COLUMNS.size(), body.getColumns().size());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getComment(), body.getComment());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getEscape(), body.getEscape());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getDelimiter(), body.getDelimiter());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getNewlineDelimiter(), body.getNewlineDelimiter());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getQuote(), body.getQuote());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getTimestampFormat(), body.getTimestampFormat());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getDateFormat(), body.getDateFormat());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getPrompt(), body.getPrompt());
    }

}
