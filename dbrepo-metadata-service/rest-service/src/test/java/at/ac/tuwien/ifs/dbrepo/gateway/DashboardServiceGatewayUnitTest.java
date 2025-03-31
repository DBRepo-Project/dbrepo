package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardResponseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.PermissionTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DashboardServiceGatewayUnitTest extends BaseTest {

    @MockBean
    @Qualifier("dashboardServiceRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private DashboardServiceGateway dashboardServiceGateway;

    @Test
    public void update_succeeds() throws DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                        .build());

        /* test */
        dashboardServiceGateway.update(DATABASE_2_DTO);
    }

    @Test
    public void update_wrongStatus_failed() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.update(DATABASE_1_DTO);
        });
    }

    @Test
    public void update_connection_failed() {

        /* mock */
        doThrow(HttpServerErrorException.ServiceUnavailable.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DashboardServiceConnectionException.class, () -> {
            dashboardServiceGateway.update(DATABASE_1_DTO);
        });
    }

    @Test
    public void update_unauthorized_failed() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.update(DATABASE_1_DTO);
        });
    }

    @Test
    public void create_succeeds() throws DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CreateDashboardResponseDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(DATABASE_2_CREATE_DASHBOARD_RESPONSE_DTO));

        /* test */
        final CreateDashboardResponseDto response = dashboardServiceGateway.create(DATABASE_2_CREATE_DASHBOARD_DTO);
        assertEquals(DATABASE_2_DASHBOARD_UID, response.getUid());
    }

    @Test
    public void create_emptyBody_fails() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CreateDashboardResponseDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.CREATED)
                        .body(null)); // <<<

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.create(DATABASE_2_CREATE_DASHBOARD_DTO);
        });
    }

    @Test
    public void create_wrongStatus_failed() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CreateDashboardResponseDto.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.create(DATABASE_2_CREATE_DASHBOARD_DTO);
        });
    }

    @Test
    public void create_connection_failed() {

        /* mock */
        doThrow(HttpServerErrorException.ServiceUnavailable.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CreateDashboardResponseDto.class));

        /* test */
        assertThrows(DashboardServiceConnectionException.class, () -> {
            dashboardServiceGateway.create(DATABASE_2_CREATE_DASHBOARD_DTO);
        });
    }

    @Test
    public void create_unauthorized_failed() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CreateDashboardResponseDto.class));

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.create(DATABASE_2_CREATE_DASHBOARD_DTO);
        });
    }

    @Test
    public void create_exists_failed() {

        /* mock */
        doThrow(HttpClientErrorException.Conflict.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), eq(CreateDashboardResponseDto.class));

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.create(DATABASE_2_CREATE_DASHBOARD_DTO);
        });
    }

    @Test
    public void updateAccess_succeeds() throws DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                        .build());

        /* test */
        dashboardServiceGateway.updateAccess(DATABASE_2_DASHBOARD_UID, USER_2_USERNAME, PermissionTypeDto.VIEW);
    }

    @Test
    public void updateAccess_wrongStatus_failed() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.updateAccess(DATABASE_2_DASHBOARD_UID, USER_2_USERNAME, PermissionTypeDto.VIEW);
        });
    }

    @Test
    public void updateAccess_connection_failed() {

        /* mock */
        doThrow(HttpServerErrorException.ServiceUnavailable.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DashboardServiceConnectionException.class, () -> {
            dashboardServiceGateway.updateAccess(DATABASE_2_DASHBOARD_UID, USER_2_USERNAME, PermissionTypeDto.VIEW);
        });
    }

    @Test
    public void updateAccess_unauthorized_failed() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.updateAccess(DATABASE_2_DASHBOARD_UID, USER_2_USERNAME, PermissionTypeDto.VIEW);
        });
    }

    @Test
    public void updateAnonymousAccess_succeeds() throws DashboardServiceException, DashboardServiceConnectionException {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.ACCEPTED)
                        .build());

        /* test */
        dashboardServiceGateway.updateAnonymousAccess(DATABASE_2_DASHBOARD_UID, DATABASE_2_BRIEF_DTO);
    }

    @Test
    public void updateAnonymousAccess_wrongStatus_failed() {

        /* mock */
        when(restTemplate.exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class)))
                .thenReturn(ResponseEntity.status(HttpStatus.OK)
                        .build());

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.updateAnonymousAccess(DATABASE_2_DASHBOARD_UID, DATABASE_2_BRIEF_DTO);
        });
    }

    @Test
    public void updateAnonymousAccess_connection_failed() {

        /* mock */
        doThrow(HttpServerErrorException.ServiceUnavailable.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DashboardServiceConnectionException.class, () -> {
            dashboardServiceGateway.updateAnonymousAccess(DATABASE_2_DASHBOARD_UID, DATABASE_2_BRIEF_DTO);
        });
    }

    @Test
    public void updateAnonymousAccess_unauthorized_failed() {

        /* mock */
        doThrow(HttpClientErrorException.Unauthorized.class)
                .when(restTemplate)
                .exchange(anyString(), eq(HttpMethod.PUT), any(HttpEntity.class), eq(Void.class));

        /* test */
        assertThrows(DashboardServiceException.class, () -> {
            dashboardServiceGateway.updateAnonymousAccess(DATABASE_2_DASHBOARD_UID, DATABASE_2_BRIEF_DTO);
        });
    }
}