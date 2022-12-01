package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.ExportResource;
import at.tuwien.api.database.ViewBriefDto;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.DatabaseAccessRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.repository.jpa.ViewRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
    private ViewRepository viewRepository;

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
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }
        if (principal == null) {
            when(viewRepository.findAllPublicByDatabaseId(databaseId))
                    .thenReturn(List.of(VIEW_1));
        } else {
            when(viewRepository.findAllPublicOrMineByDatabaseId(databaseId, username))
                    .thenReturn(List.of(VIEW_1));
        }

        /* test */
        final ResponseEntity<List<ViewBriefDto>> response = viewEndpoint.findAll(CONTAINER_1_ID, DATABASE_1_ID, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().size());
    }

}
