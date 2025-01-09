package at.tuwien.gateway;

import at.tuwien.api.container.ContainerDto;
import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.DatabaseAccessDto;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedViewDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.internal.PrivilegedTableDto;
import at.tuwien.api.identifier.IdentifierBriefDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.api.user.internal.PrivilegedUserDto;
import at.tuwien.exception.*;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MetadataServiceGatewayUnitTest extends AbstractUnitTest {

    @MockBean
    @Qualifier("restTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private MetadataServiceGateway metadataServiceGateway;

    @BeforeEach
    public void beforeEach() {
        genesis();
    }

    @Test
    public void getTableById_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Type", IMAGE_1_JDBC);
        headers.set("X-Host", CONTAINER_1_HOST);
        headers.set("X-Port", "" + CONTAINER_1_PORT);
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);
        headers.set("X-Database", DATABASE_1_INTERNALNAME);
        headers.set("X-Table", TABLE_1_INTERNAL_NAME);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .headers(headers)
                        .body(TABLE_1_DTO));

        /* test */
        final PrivilegedTableDto response = metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
        assertEquals(IMAGE_1_JDBC, response.getDatabase().getContainer().getImage().getJdbcMethod());
        assertEquals(CONTAINER_1_HOST, response.getDatabase().getContainer().getHost());
        assertEquals(CONTAINER_1_PORT, response.getDatabase().getContainer().getPort());
        assertEquals(CONTAINER_1_PRIVILEGED_USERNAME, response.getDatabase().getContainer().getUsername());
        assertEquals(CONTAINER_1_PRIVILEGED_PASSWORD, response.getDatabase().getContainer().getPassword());
        assertEquals(DATABASE_1_INTERNALNAME, response.getDatabase().getInternalName());
        assertEquals(TABLE_1_INTERNAL_NAME, response.getInternalName());
    }

    @Test
    public void getTableById_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void getTableById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.ServiceUnavailable.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void getTableById_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(TABLE_1_DTO));

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void getTableById_headerMissing_fails() {
        final List<String> customHeaders = List.of("X-Type", "X-Host", "X-Port", "X-Username", "X-Password", "X-Database", "X-Sidecar-Host", "X-Sidecar-Port");

        for (int i = 0; i < customHeaders.size(); i++) {
            final HttpHeaders headers = new HttpHeaders();
            for (int j = 0; j < i; j++) {
                headers.add(customHeaders.get(j), "");
            }
            /* mock */
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                    .thenReturn(ResponseEntity.status(HttpStatus.OK)
                            .headers(headers)
                            .body(TABLE_1_DTO));
            /* test */
            assertThrows(MetadataServiceException.class, () -> {
                metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
            });
        }
    }

    @Test
    public void getTableById_emptyBody_fails() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Type", IMAGE_1_JDBC);
        headers.set("X-Host", CONTAINER_1_HOST);
        headers.set("X-Port", "" + CONTAINER_1_PORT);
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);
        headers.set("X-Database", DATABASE_1_INTERNALNAME);
        headers.set("X-Table", TABLE_1_INTERNAL_NAME);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .headers(headers)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void getDatabaseById_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            DatabaseNotFoundException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);
        headers.set("X-Host", CONTAINER_1_HOST);
        headers.set("X-Port", "" + CONTAINER_1_PORT);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(PrivilegedDatabaseDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .body(DATABASE_1_PRIVILEGED_DTO));

        /* test */
        final PrivilegedDatabaseDto response = metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void getDatabaseById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(PrivilegedDatabaseDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
        });
    }

    @Test
    public void getDatabaseById_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(PrivilegedDatabaseDto.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
        });
    }

    @Test
    public void getDatabaseById_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(PrivilegedDatabaseDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
        });
    }

    @Test
    public void getDatabaseById_emptyBody_fails() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(PrivilegedDatabaseDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .headers(headers)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
        });
    }

    @Test
    public void getDatabaseById_headerMissing_fails() {
        final List<String> customHeaders = List.of("X-Username", "X-Password");

        for (int i = 0; i < customHeaders.size(); i++) {
            final HttpHeaders headers = new HttpHeaders();
            for (int j = 0; j < i; j++) {
                headers.add(customHeaders.get(j), "");
            }
            /* mock */
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(PrivilegedDatabaseDto.class)))
                    .thenReturn(ResponseEntity.status(HttpStatus.OK)
                            .headers(headers)
                            .build());
            /* test */
            assertThrows(MetadataServiceException.class, () -> {
                metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
            });
        }
    }

    @Test
    public void getContainerById_succeeds() throws RemoteUnavailableException, ContainerNotFoundException, MetadataServiceException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .body(CONTAINER_1_DTO));

        /* test */
        final PrivilegedContainerDto response = metadataServiceGateway.getContainerById(CONTAINER_1_ID);
        assertEquals(CONTAINER_1_ID, response.getId());
    }

    @Test
    public void getContainerById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getContainerById(CONTAINER_1_ID);
        });
    }

    @Test
    public void getContainerById_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class));

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            metadataServiceGateway.getContainerById(CONTAINER_1_ID);
        });
    }

    @Test
    public void getContainerById_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getContainerById(CONTAINER_1_ID);
        });
    }

    @Test
    public void getContainerById_headerMissing_fails() {
        final List<String> customHeaders = List.of("X-Username", "X-Password");

        for (int i = 0; i < customHeaders.size(); i++) {
            final HttpHeaders headers = new HttpHeaders();
            for (int j = 0; j < i; j++) {
                headers.add(customHeaders.get(j), "");
            }
            /* mock */
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class)))
                    .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                            .build());

            /* test */
            assertThrows(MetadataServiceException.class, () -> {
                metadataServiceGateway.getContainerById(CONTAINER_1_ID);
            });
        }
    }

    @Test
    public void getContainerById_emptyBody_fails() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .headers(headers)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getContainerById(CONTAINER_1_ID);
        });
    }

    @Test
    public void getViewById_succeeds() throws RemoteUnavailableException, ViewNotFoundException, MetadataServiceException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Type", IMAGE_1_JDBC);
        headers.set("X-Host", CONTAINER_1_HOST);
        headers.set("X-Port", "" + CONTAINER_1_PORT);
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);
        headers.set("X-Database", DATABASE_1_INTERNALNAME);
        headers.set("X-View", VIEW_1_INTERNAL_NAME);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .body(VIEW_1_DTO));

        /* test */
        final PrivilegedViewDto response = metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
        assertEquals(VIEW_1_ID, response.getId());
    }

    @Test
    public void getViewById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void getViewById_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class));

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void getViewById_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void getViewById_headerMissing_fails() {
        final List<String> customHeaders = List.of("X-Type", "X-Host", "X-Port", "X-Username", "X-Password", "X-Database");

        for (int i = 0; i < customHeaders.size(); i++) {
            final HttpHeaders headers = new HttpHeaders();
            for (int j = 0; j < i; j++) {
                headers.add(customHeaders.get(j), "");
            }
            /* mock */
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class)))
                    .thenReturn(ResponseEntity.status(HttpStatus.OK)
                            .build());

            /* test */
            assertThrows(MetadataServiceException.class, () -> {
                metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
            });
        }
    }

    @Test
    public void getViewById_emptyBody_fails() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Type", IMAGE_1_JDBC);
        headers.set("X-Host", CONTAINER_1_HOST);
        headers.set("X-Port", "" + CONTAINER_1_PORT);
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);
        headers.set("X-Database", DATABASE_1_INTERNALNAME);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void getUserById_succeeds() throws RemoteUnavailableException, UserNotFoundException, MetadataServiceException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .body(USER_1_DTO));

        /* test */
        final UserDto response = metadataServiceGateway.getUserById(USER_1_ID);
        assertEquals(USER_1_ID, response.getId());
    }

    @Test
    public void getUserById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getUserById(USER_1_ID);
        });
    }

    @Test
    public void getUserById_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            metadataServiceGateway.getUserById(USER_1_ID);
        });
    }

    @Test
    public void getUserById_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getUserById(USER_1_ID);
        });
    }

    @Test
    public void getUserById_emptyBody_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getUserById(USER_1_ID);
        });
    }

    @Test
    public void getPrivilegedUserById_succeeds() throws RemoteUnavailableException, UserNotFoundException,
            MetadataServiceException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .body(USER_1_DTO));

        /* test */
        final PrivilegedUserDto response = metadataServiceGateway.getPrivilegedUserById(USER_1_ID);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(CONTAINER_1_PRIVILEGED_USERNAME, response.getUsername());
        assertEquals(CONTAINER_1_PRIVILEGED_PASSWORD, response.getPassword());
    }

    @Test
    public void getPrivilegedUserById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getPrivilegedUserById(USER_1_ID);
        });
    }

    @Test
    public void getPrivilegedUserById_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            metadataServiceGateway.getPrivilegedUserById(USER_1_ID);
        });
    }

    @Test
    public void getPrivilegedUserById_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getPrivilegedUserById(USER_1_ID);
        });
    }

    @Test
    public void getPrivilegedUserById_headerMissing_fails() {
        final List<String> customHeaders = List.of("X-Username", "X-Password");

        for (int i = 0; i < customHeaders.size(); i++) {
            final HttpHeaders headers = new HttpHeaders();
            for (int j = 0; j < i; j++) {
                headers.add(customHeaders.get(j), "");
            }
            /* mock */
            when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                    .thenReturn(ResponseEntity.status(HttpStatus.OK)
                            .build());

            /* test */
            assertThrows(MetadataServiceException.class, () -> {
                metadataServiceGateway.getPrivilegedUserById(USER_1_ID);
            });
        }
    }

    @Test
    public void getPrivilegedUserById_emptyBody_fails() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getPrivilegedUserById(USER_1_ID);
        });
    }

    @Test
    public void getAccess_succeeds() throws RemoteUnavailableException, NotAllowedException, MetadataServiceException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .body(DATABASE_1_USER_1_READ_ACCESS_DTO));

        /* test */
        final DatabaseAccessDto response = metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID);
    }

    @Test
    public void getAccess_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    public void getAccess_forbidden_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Forbidden.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    public void getAccess_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    public void getAccess_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    public void getAccess_emptyBody_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    public void getIdentifiers_witSubset_succeeds() throws RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class)))
                .thenReturn(ResponseEntity.ok()
                        .body(new IdentifierBriefDto[]{IDENTIFIER_1_BRIEF_DTO}));

        /* test */
        final List<IdentifierBriefDto> response = metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void getIdentifiers_succeeds() throws RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class)))
                .thenReturn(ResponseEntity.ok()
                        .body(new IdentifierBriefDto[]{IDENTIFIER_1_BRIEF_DTO}));

        /* test */
        final List<IdentifierBriefDto> response = metadataServiceGateway.getIdentifiers(DATABASE_1_ID, null);
        assertEquals(1, response.size());
    }

    @Test
    public void getIdentifiers_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void getIdentifiers_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void getIdentifiers_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void getIdentifiers_emptyBody_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class)))
                .thenReturn(ResponseEntity.ok()
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void updateTableStatistics_succeeds() throws RemoteUnavailableException, MetadataServiceException, TableNotFoundException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        metadataServiceGateway.updateTableStatistics(DATABASE_1_ID, TABLE_1_ID, TOKEN_ACCESS_TOKEN);
    }

    @Test
    public void updateTableStatistics_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.updateTableStatistics(DATABASE_1_ID, TABLE_1_ID, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    public void updateTableStatistics_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), eq(HttpEntity.EMPTY), eq(Void.class));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            metadataServiceGateway.updateTableStatistics(DATABASE_1_ID, TABLE_1_ID, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    public void updateTableStatistics_statusCode_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), eq(HttpEntity.EMPTY), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.updateTableStatistics(DATABASE_1_ID, TABLE_1_ID, TOKEN_ACCESS_TOKEN);
        });
    }

}
