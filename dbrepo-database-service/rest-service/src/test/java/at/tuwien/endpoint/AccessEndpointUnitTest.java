package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.endpoints.AccessEndpoint;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import at.tuwien.mapper.AccessMapper;
import at.tuwien.repository.jpa.*;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AccessEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private Channel channel;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private AccessEndpoint accessEndpoint;

    @Autowired
    private AccessMapper accessMapper;

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, USER_2_USERNAME, USER_2, null);
        });
    }

    @Test
    @Disabled("not unit test")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-access"})
    public void create_hasRoleNoAccess_succeeds() throws UserNotFoundException, NotAllowedException,
            QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException {

        /* test */
        generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, USER_1_USERNAME, USER_1, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void create_noRoleNoAccess_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, USER_3_USERNAME, USER_3, USER_3_PRINCIPAL);
        });
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_find(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, USER_1_USERNAME, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"check-database-access"})
    public void find_hasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_find(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, USER_2_USERNAME, USER_2_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"check-database-access"})
    public void find_hasRoleHasAccess_fails() throws AccessDeniedException, NotAllowedException {

        /* test */
        generic_find(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, DATABASE_1_RESEARCHER_READ_ACCESS, USER_2_USERNAME, USER_2_ID, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void update_anonymous_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_update(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, USER_3_USERNAME, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-database-access"})
    public void update_hasRoleNoAccess_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_update(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, USER_3_USERNAME, USER_1_PRINCIPAL);
        });
    }

    @Test
    @Disabled("not unit test")
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-database-access"})
    public void update_hasRoleHasAccess_succeeds() throws UserNotFoundException, AccessDeniedException,
            NotAllowedException, QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        generic_update(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, DATABASE_1_DATA_STEWARD_READ_ACCESS, USER_3_USERNAME, USER_1_PRINCIPAL);
    }

    @Test
    @WithMockUser(username = USER_3_USERNAME)
    public void update_noRoleNoAccess_fails() {

        /* mock */
        when(userRepository.findByUsername(USER_3_USERNAME))
                .thenReturn(Optional.of(USER_3));

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_update(CONTAINER_1_ID, DATABASE_1_ID, DATABASE_1, null, USER_3_USERNAME, USER_3_PRINCIPAL);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(Long containerId, Long databaseId, Database database, DatabaseAccess access,
                                  String username, User user, Principal principal) throws UserNotFoundException,
            NotAllowedException, QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .username(username)
                .type(AccessTypeDto.READ)
                .build();

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        when(userRepository.findByUsername(username))
                .thenReturn(Optional.of(user));
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }

        /* test */
        final ResponseEntity<?> response = accessEndpoint.create(containerId, databaseId, request, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    protected void generic_find(Long containerId, Long databaseId, Database database, DatabaseAccess access,
                                String username, String userId, Principal principal) throws AccessDeniedException,
            NotAllowedException {

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (access != null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, principal.getName()))
                    .thenReturn(Optional.of(DatabaseAccess.builder()
                            .type(access.getType())
                            .hdbid(databaseId)
                            .huserid(username.equals(USER_1_USERNAME) ? USER_1_ID : USER_2_ID)
                            .build()));
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        }

        /* test */
        final ResponseEntity<DatabaseAccessDto> response = accessEndpoint.find(containerId, databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final DatabaseAccessDto dto = response.getBody();
        assertEquals(userId, dto.getHuserid());
        assertEquals(databaseId, dto.getHdbid());
        assertEquals(accessMapper.accessType(access.getType()), dto.getType());
    }

    protected void generic_update(Long containerId, Long databaseId, Database database, DatabaseAccess access,
                                  String username, Principal principal) throws AccessDeniedException,
            NotAllowedException, UserNotFoundException, QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(AccessTypeDto.READ)
                .build();

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (access == null) {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.empty());
        } else {
            when(databaseAccessRepository.findByDatabaseIdAndUsername(databaseId, username))
                    .thenReturn(Optional.of(access));
        }

        /* test */
        final ResponseEntity<?> response = accessEndpoint.update(containerId, databaseId, username, request, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
