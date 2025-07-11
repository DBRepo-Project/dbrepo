package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.container.ContainerDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.ViewDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierBriefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.*;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MetadataServiceGatewayUnitTest extends BaseTest {

    @MockitoBean
    @Qualifier("internalRestTemplate")
    private RestTemplate internalRestTemplate;

    @Autowired
    private MetadataServiceGateway metadataServiceGateway;

    @Test
    public void getTableById_succeeds() throws TableNotFoundException, RemoteUnavailableException,
            MetadataServiceException {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .body(TABLE_1_DTO));

        /* test */
        final TableDto response = metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
        assertEquals(TABLE_1_INTERNAL_NAME, response.getInternalName());
    }

    @Test
    public void getTableById_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(internalRestTemplate)
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
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void getTableById_statusCode_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body(TABLE_1_DTO));

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getTableById(DATABASE_1_ID, TABLE_1_ID);
        });
    }

    @Test
    public void getTableById_emptyBody_fails() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(TableDto.class)))
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
        headers.set("X-Host", CONTAINER_1_HOST);
        headers.set("X-Port", "" + CONTAINER_1_PORT);
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);
        headers.set("X-Jdbc-Method", IMAGE_1_JDBC_METHOD);
        headers.set("Access-Control-Expose-Headers", "X-Username X-Password X-Jdbc-Method X-Host X-Port");

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .body(DATABASE_1_PRIVILEGED_DTO));

        /* test */
        final DatabaseDto response = metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
        assertEquals(DATABASE_1_ID, response.getId());
    }

    @Test
    public void getDatabaseById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
        });
    }

    @Test
    public void getDatabaseById_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            metadataServiceGateway.getDatabaseById(DATABASE_1_ID);
        });
    }

    @Test
    public void getDatabaseById_statusCode_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
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
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
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
            when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseDto.class)))
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
        headers.set("X-Host", CONTAINER_1_HOST);
        headers.set("X-Port", "" + CONTAINER_1_PORT);
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);
        headers.set("X-Jdbc-Method", IMAGE_1_JDBC_METHOD);
        headers.set("Access-Control-Expose-Headers", "X-Username X-Password X-Jdbc-Method X-Host X-Port");

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .body(CONTAINER_1_DTO));

        /* test */
        final ContainerDto response = metadataServiceGateway.getContainerById(CONTAINER_1_ID);
        assertEquals(CONTAINER_1_ID, response.getId());
    }

    @Test
    public void getContainerById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(internalRestTemplate)
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
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class));

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            metadataServiceGateway.getContainerById(CONTAINER_1_ID);
        });
    }

    @Test
    public void getContainerById_statusCode_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class)))
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
            when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class)))
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
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ContainerDto.class)))
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

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(ViewDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .body(VIEW_1_DTO));

        /* test */
        final ViewDto response = metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
        assertEquals(VIEW_1_ID, response.getId());
    }

    @Test
    public void getViewById_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(internalRestTemplate)
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
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class));

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void getViewById_statusCode_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class)))
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
            when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class)))
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
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(ViewDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getViewById(CONTAINER_1_ID, VIEW_1_ID);
        });
    }

    @Test
    public void getUserByUsername_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getUserByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void getUserByUsername_succeeds() throws RemoteUnavailableException, UserNotFoundException,
            MetadataServiceException {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .body(USER_1_DTO));

        /* test */
        final UserDto response = metadataServiceGateway.getUserByUsername(USER_1_USERNAME);
        assertEquals(USER_1_ID, response.getId());
        assertEquals(CONTAINER_1_PRIVILEGED_USERNAME, response.getUsername());
        assertEquals(CONTAINER_1_PRIVILEGED_PASSWORD, response.getPassword());
    }

    @Test
    public void getUserByUsername_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class));

        /* test */
        assertThrows(UserNotFoundException.class, () -> {
            metadataServiceGateway.getUserByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void getUserByUsername_statusCode_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getUserByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void getUserByUsername_headerMissing_fails() {
        final List<String> customHeaders = List.of("X-Username", "X-Password");

        for (int i = 0; i < customHeaders.size(); i++) {
            final HttpHeaders headers = new HttpHeaders();
            for (int j = 0; j < i; j++) {
                headers.add(customHeaders.get(j), "");
            }
            /* mock */
            when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                    .thenReturn(ResponseEntity.status(HttpStatus.OK)
                            .build());

            /* test */
            assertThrows(MetadataServiceException.class, () -> {
                metadataServiceGateway.getUserByUsername(USER_1_USERNAME);
            });
        }
    }

    @Test
    public void getUserByUsername_emptyBody_fails() {
        final HttpHeaders headers = new HttpHeaders();
        headers.set("X-Username", CONTAINER_1_PRIVILEGED_USERNAME);
        headers.set("X-Password", CONTAINER_1_PRIVILEGED_PASSWORD);

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(UserDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .headers(headers)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getUserByUsername(USER_1_USERNAME);
        });
    }

    @Test
    public void getAccess_succeeds() throws RemoteUnavailableException, NotAllowedException, MetadataServiceException {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .body(DATABASE_1_USER_1_READ_ACCESS_DTO));

        /* test */
        final DatabaseAccessDto response = metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_USERNAME);
    }

    @Test
    public void getAccess_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void getAccess_forbidden_fails() {

        /* mock */
        doThrow(HttpClientErrorException.Forbidden.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void getAccess_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void getAccess_statusCode_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void getAccess_emptyBody_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(DatabaseAccessDto.class)))
                .thenReturn(ResponseEntity.ok()
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getAccess(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void getIdentifiers_witSubset_succeeds() throws RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class)))
                .thenReturn(ResponseEntity.ok()
                        .body(new IdentifierBriefDto[]{IDENTIFIER_1_BRIEF_DTO}));

        /* test */
        final List<IdentifierBriefDto> response = metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void getIdentifiers_succeeds() throws RemoteUnavailableException, DatabaseNotFoundException, MetadataServiceException {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class)))
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
                .when(internalRestTemplate)
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
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class));

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void getIdentifiers_statusCode_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class)))
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
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.GET), eq(HttpEntity.EMPTY), eq(IdentifierBriefDto[].class)))
                .thenReturn(ResponseEntity.ok()
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.getIdentifiers(DATABASE_1_ID, QUERY_1_ID);
        });
    }

    @Test
    public void updateTableStatistics_succeeds() throws RemoteUnavailableException, MetadataServiceException,
            TableNotFoundException {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.accepted()
                        .build());

        /* test */
        metadataServiceGateway.updateTableStatistics(DATABASE_1_ID, TABLE_1_ID, TOKEN_ACCESS_TOKEN);
    }

    @Test
    public void updateTableStatistics_unavailable_fails() {

        /* mock */
        doThrow(HttpServerErrorException.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataServiceGateway.updateTableStatistics(DATABASE_1_ID, TABLE_1_ID, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    public void updateTableStatistics_notFound_fails() {

        /* mock */
        doThrow(HttpClientErrorException.NotFound.class)
                .when(internalRestTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            metadataServiceGateway.updateTableStatistics(DATABASE_1_ID, TABLE_1_ID, TOKEN_ACCESS_TOKEN);
        });
    }

    @Test
    public void updateTableStatistics_statusCode_fails() {

        /* mock */
        when(internalRestTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .build());

        /* test */
        assertThrows(MetadataServiceException.class, () -> {
            metadataServiceGateway.updateTableStatistics(DATABASE_1_ID, TABLE_1_ID, TOKEN_ACCESS_TOKEN);
        });
    }

}
