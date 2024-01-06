package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.DatabaseGiveAccessDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.*;
import at.tuwien.mapper.AccessMapper;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.AccessService;
import lombok.extern.log4j.Log4j2;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class AccessEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private AccessService accessService;

    @MockBean
    private DatabaseRepository databaseRepository;

    @Autowired
    private AccessEndpoint accessEndpoint;

    @Autowired
    private AccessMapper accessMapper;

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, null, USER_2_ID, null);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRoleNoAccess_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_create(DATABASE_1_ID, DATABASE_1, null, USER_4_ID, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-database-access"})
    public void create_succeeds() throws UserNotFoundException, QueryMalformedException, DatabaseNotFoundException,
            DatabaseMalformedException, NotAllowedException, KeycloakRemoteException, AccessDeniedException {

        /* mock */
        doNothing()
                .when(accessService)
                .create(eq(DATABASE_1_ID), eq(USER_2_ID), any(DatabaseGiveAccessDto.class));

        /* test */
        generic_create(DATABASE_1_ID, DATABASE_1, null, USER_2_ID, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_find(DATABASE_1_ID, DATABASE_1, null, null, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"check-database-access"})
    public void find_hasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_find(DATABASE_1_ID, DATABASE_1, null, USER_2_ID, USER_2_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"check-database-access"})
    public void find_hasRoleHasAccess_succeeds() throws NotAllowedException, AccessDeniedException,
            DatabaseNotFoundException {

        /* test */
        generic_find(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_1_READ_ACCESS, USER_1_ID, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void update_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_update(DATABASE_1_ID, DATABASE_1, null, USER_4_ID, null);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-database-access"})
    public void update_hasRoleNoAccess_fails() {

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            generic_update(DATABASE_1_ID, DATABASE_1, null, USER_4_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void update_noRoleNoAccess_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_update(DATABASE_1_ID, DATABASE_1, null, USER_4_ID, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"update-database-access"})
    public void update_succeeds() throws UserNotFoundException, NotAllowedException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, KeycloakRemoteException, AccessDeniedException {

        /* mock */
        doNothing()
                .when(accessService)
                .update(eq(DATABASE_1_ID), eq(USER_2_ID), any(DatabaseModifyAccessDto.class));

        /* test */
        generic_update(DATABASE_1_ID, DATABASE_1, DATABASE_1_USER_2_WRITE_OWN_ACCESS, USER_2_ID, USER_1_PRINCIPAL);
    }

    @Test
    @WithAnonymousUser
    public void revoke_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_revoke(DATABASE_1_ID, DATABASE_1_USER_1_WRITE_ALL_ACCESS, USER_2_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void revoke_noRoleNoAccess_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            generic_revoke(DATABASE_1_ID, DATABASE_1_USER_1_WRITE_ALL_ACCESS, USER_2_ID, USER_4_PRINCIPAL);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"delete-database-access"})
    public void revoke_succeeds() throws UserNotFoundException, QueryMalformedException, DatabaseNotFoundException,
            DatabaseMalformedException, NotAllowedException, AccessDeniedException {

        /* mock */
        doNothing()
                .when(accessService)
                .delete(DATABASE_1_ID, USER_2_ID);

        /* test */
        generic_revoke(DATABASE_1_ID, DATABASE_1_USER_1_WRITE_ALL_ACCESS, USER_2_ID, USER_1_PRINCIPAL);
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void generic_create(Long databaseId, Database database, DatabaseAccess access, UUID userId,
                                  Principal principal) throws UserNotFoundException, QueryMalformedException,
            DatabaseNotFoundException, DatabaseMalformedException, NotAllowedException, KeycloakRemoteException,
            AccessDeniedException {
        final DatabaseGiveAccessDto request = DatabaseGiveAccessDto.builder()
                .type(AccessTypeDto.READ)
                .build();

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (access != null) {
            log.trace("mock access {} for user with id {} for database with id {}", access.getType(), userId, databaseId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for user with id {} for database with id {}", userId, databaseId);
            doThrow(AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }

        /* test */
        final ResponseEntity<?> response = accessEndpoint.create(databaseId, userId, request, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    protected void generic_find(Long databaseId, Database database, DatabaseAccess access, UUID userId,
                                Principal principal) throws NotAllowedException, AccessDeniedException,
            DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (access != null) {
            log.trace("mock access {} for user with id {} for database with id {}", access.getType(), userId, databaseId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for user with id {} for database with id {}", userId, databaseId);
            doThrow(AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }

        /* test */
        final ResponseEntity<DatabaseAccessDto> response = accessEndpoint.find(databaseId, principal);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final DatabaseAccessDto dto = response.getBody();
        assertEquals(userId, dto.getHuserid());
        assertEquals(databaseId, dto.getHdbid());
        assertEquals(accessMapper.accessType(access.getType()), dto.getType());
    }

    protected void generic_update(Long databaseId, Database database, DatabaseAccess access, UUID userId,
                                  Principal principal) throws NotAllowedException, UserNotFoundException,
            QueryMalformedException, DatabaseNotFoundException, DatabaseMalformedException, AccessDeniedException,
            KeycloakRemoteException {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(AccessTypeDto.READ)
                .build();

        /* mock */
        when(databaseRepository.findById(databaseId))
                .thenReturn(Optional.of(database));
        if (access != null) {
            log.trace("mock access {} for user with id {} for database with id {}", access.getType(), userId, databaseId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for user with id {} for database with id {}", userId, databaseId);
            doThrow(AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }

        /* test */
        final ResponseEntity<?> response = accessEndpoint.update(databaseId, userId, request, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

    protected void generic_revoke(Long databaseId, DatabaseAccess access, UUID userId, Principal principal)
            throws NotAllowedException, UserNotFoundException, QueryMalformedException, DatabaseNotFoundException,
            DatabaseMalformedException, AccessDeniedException {

        /* mock */
        if (access != null) {
            log.trace("mock access {} for user with id {} for database with id {}", access.getType(), userId, databaseId);
            when(accessService.find(databaseId, userId))
                    .thenReturn(access);
        } else {
            log.trace("mock no access for user with id {} for database with id {}", userId, databaseId);
            doThrow(AccessDeniedException.class)
                    .when(accessService)
                    .find(databaseId, userId);
        }

        /* test */
        final ResponseEntity<?> response = accessEndpoint.revoke(databaseId, userId, principal);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }

}
