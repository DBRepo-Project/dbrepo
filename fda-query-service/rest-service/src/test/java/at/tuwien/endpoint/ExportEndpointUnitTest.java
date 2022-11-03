package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.service.DatabaseService;
import at.tuwien.service.QueryService;
import at.tuwien.service.TableService;
import at.tuwien.service.impl.QueryServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.apache.http.impl.io.EmptyInputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ExportEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @Autowired
    private ExportEndpoint exportEndpoint;

    @MockBean
    private QueryService queryService;

    @MockBean
    private DatabaseService databaseService;

    @Test
    public void export_timestampNull_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        doReturn(DATABASE_1, DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);
        doReturn(
                ExportResource.builder()
                        .resource(new InputStreamResource(EmptyInputStream.nullInputStream()))
                        .filename("/tmp/filename")
                        .build()
        ).when(queryService)
                .findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, null);

        /* test */
        final ResponseEntity<InputStreamResource> response = exportEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID,
                TABLE_1_ID, null, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    public void export_succeeds() throws TableNotFoundException, DatabaseConnectionException, TableMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, FileStorageException, PaginationException,
            ContainerNotFoundException, NotAllowedException, QueryMalformedException {
        final Instant request = Instant.now()
                .minusMillis(1000 * 1000);
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        doReturn(DATABASE_1, DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);
        doReturn(
                ExportResource.builder()
                        .resource(new InputStreamResource(EmptyInputStream.nullInputStream()))
                        .filename("/tmp/filename")
                        .build()
        ).when(queryService)
                .findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request);

        /* test */
        final ResponseEntity<InputStreamResource> response = exportEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID,
                TABLE_1_ID, request, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());

    }

    @Test
    public void export_inFuture_succeeds() throws TableNotFoundException, DatabaseConnectionException,
            TableMalformedException, DatabaseNotFoundException, ImageNotSupportedException, FileStorageException,
            PaginationException, ContainerNotFoundException, NotAllowedException, QueryMalformedException {
        final Instant request = Instant.now()
                .plusMillis(1000 * 1000);
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        doReturn(DATABASE_1, DATABASE_1).when(databaseService)
                .find(CONTAINER_1_ID, DATABASE_1_ID);
        doReturn(
                ExportResource.builder()
                        .resource(new InputStreamResource(EmptyInputStream.nullInputStream()))
                        .filename("/tmp/filename")
                        .build()
        ).when(queryService)
                .findAll(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID, request);

        /* test */
        final ResponseEntity<InputStreamResource> response = exportEndpoint.export(CONTAINER_1_ID, DATABASE_1_ID,
                TABLE_1_ID, request, principal);
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

}
