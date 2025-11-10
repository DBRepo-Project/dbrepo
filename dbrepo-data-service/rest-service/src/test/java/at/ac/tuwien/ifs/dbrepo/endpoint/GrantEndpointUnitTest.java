package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseGrantsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.GrantTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.GrantEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.GrantService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class GrantEndpointUnitTest extends BaseTest {

    @Autowired
    private GrantEndpoint grantEndpoint;

    @MockitoBean
    private HttpServletRequest httpServletRequest;

    @MockitoBean
    private MetadataService metadataService;

    @MockitoBean
    private GrantService grantService;

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_succeeds() throws UserNotFoundException, DatabaseUnavailableException, NotAllowedException,
            DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException,
            DatabaseMalformedException, SQLException, AccessNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);
        when(grantService.find(DATABASE_1_CACHE, USER_1_CACHE))
                .thenReturn(READ_GRANT_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<DatabaseGrantsDto> response = grantEndpoint.find(DATABASE_1_ID, USER_1_USERNAME, USER_1_PRINCIPAL, httpServletRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final DatabaseGrantsDto body = response.getBody();
        assertEquals(GrantTypeDto.READ, body.getType());
        assertNotNull(body.getGrants());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_own_succeeds() throws UserNotFoundException, DatabaseUnavailableException, NotAllowedException,
            DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException,
            DatabaseMalformedException, SQLException, AccessNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);
        when(grantService.find(DATABASE_2_CACHE, USER_1_CACHE))
                .thenReturn(READ_GRANT_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<DatabaseGrantsDto> response = grantEndpoint.find(DATABASE_2_ID, USER_1_USERNAME, USER_1_PRINCIPAL, httpServletRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final DatabaseGrantsDto body = response.getBody();
        assertEquals(GrantTypeDto.READ, body.getType());
        assertNotNull(body.getGrants());
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_head_succeeds() throws UserNotFoundException, DatabaseUnavailableException, NotAllowedException,
            DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException,
            DatabaseMalformedException, SQLException, AccessNotFoundException {

        /* mock */
        when(metadataService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);
        when(grantService.find(DATABASE_1_CACHE, USER_1_CACHE))
                .thenReturn(READ_GRANT_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<DatabaseGrantsDto> response = grantEndpoint.find(DATABASE_1_ID, USER_1_USERNAME, USER_1_PRINCIPAL, httpServletRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_notOwnerForeign_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(any(UUID.class)))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_2_USERNAME))
                .thenReturn(USER_2_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            grantEndpoint.find(DATABASE_1_ID, USER_1_USERNAME, USER_2_PRINCIPAL, httpServletRequest);
        });
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_foreign_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(metadataService.getDatabase(any(UUID.class)))
                .thenReturn(DATABASE_1_CACHE);
        when(metadataService.getUser(USER_4_USERNAME))
                .thenReturn(USER_4_CACHE);
        when(metadataService.getUser(USER_1_USERNAME))
                .thenReturn(USER_1_CACHE);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            grantEndpoint.find(DATABASE_1_ID, USER_4_USERNAME, USER_2_PRINCIPAL, httpServletRequest);
        });
    }

}
