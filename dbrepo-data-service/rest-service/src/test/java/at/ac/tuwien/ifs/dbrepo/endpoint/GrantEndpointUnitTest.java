package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseGrantsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.GrantTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.GrantEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
import at.ac.tuwien.ifs.dbrepo.service.GrantService;
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
    private CacheService cacheService;

    @MockitoBean
    private GrantService grantService;

    @Test
    @WithMockUser(username = USER_1_USERNAME)
    public void find_succeeds() throws UserNotFoundException, DatabaseUnavailableException, NotAllowedException,
            DatabaseNotFoundException, RemoteUnavailableException, MetadataServiceException,
            DatabaseMalformedException, SQLException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(cacheService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);
        when(grantService.find(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO))
                .thenReturn(READ_GRANT_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<DatabaseGrantsDto> response = grantEndpoint.find(DATABASE_1_ID, USER_1_ID, USER_1_PRINCIPAL, httpServletRequest);
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
            DatabaseMalformedException, SQLException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_2_ID))
                .thenReturn(DATABASE_2_PRIVILEGED_DTO);
        when(cacheService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);
        when(grantService.find(DATABASE_2_PRIVILEGED_DTO, USER_1_DTO))
                .thenReturn(READ_GRANT_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("GET");

        /* test */
        final ResponseEntity<DatabaseGrantsDto> response = grantEndpoint.find(DATABASE_2_ID, USER_1_ID, USER_1_PRINCIPAL, httpServletRequest);
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
            DatabaseMalformedException, SQLException {

        /* mock */
        when(cacheService.getDatabase(DATABASE_1_ID))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(cacheService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);
        when(grantService.find(DATABASE_1_PRIVILEGED_DTO, USER_1_DTO))
                .thenReturn(READ_GRANT_DTO);
        when(httpServletRequest.getMethod())
                .thenReturn("HEAD");

        /* test */
        final ResponseEntity<DatabaseGrantsDto> response = grantEndpoint.find(DATABASE_1_ID, USER_1_ID, USER_1_PRINCIPAL, httpServletRequest);
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME)
    public void find_notOwnerForeign_fails() throws UserNotFoundException, DatabaseNotFoundException,
            RemoteUnavailableException, MetadataServiceException {

        /* mock */
        when(cacheService.getDatabase(any(UUID.class)))
                .thenReturn(DATABASE_1_PRIVILEGED_DTO);
        when(cacheService.getUser(USER_2_ID))
                .thenReturn(USER_2_DTO);
        when(cacheService.getUser(USER_1_ID))
                .thenReturn(USER_1_DTO);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            grantEndpoint.find(DATABASE_1_ID, USER_1_ID, USER_2_PRINCIPAL, httpServletRequest);
        });
    }

}
