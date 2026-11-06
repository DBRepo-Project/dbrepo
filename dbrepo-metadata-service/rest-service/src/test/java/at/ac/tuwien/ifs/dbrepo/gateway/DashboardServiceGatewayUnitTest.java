package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.CreateDashboardDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.PermissionTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DashboardServiceException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.impl.DashboardServiceGatewayImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.*;
import org.springframework.web.client.*;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardServiceGatewayUnitTest extends BaseTest {

    static Stream<Object[]> failures() {
        return Stream.of("create", "update", "content", "access", "owner").flatMap(operation ->
                Stream.of(400, 401, 403, 404, 409, 500, 502, 503, 504, 0).map(status ->
                        new Object[]{operation, status}));
    }

    @ParameterizedTest
    @MethodSource("failures")
    void backendErrorsRemainDeclaredCheckedErrors(String operation, int status) {
        final RestTemplate http = mock(RestTemplate.class);
        final DashboardServiceGateway gateway = new DashboardServiceGatewayImpl(http, mock(GatewayConfig.class));
        final RestClientException error = status == 0 ? new ResourceAccessException("Synthetic timeout") :
                status < 500 ? HttpClientErrorException.create(HttpStatus.valueOf(status), "Synthetic error",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8) :
                HttpServerErrorException.create(HttpStatus.valueOf(status), "Synthetic outage",
                        HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        doThrow(error).when(http).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class));
        doThrow(error).when(http).exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class), anyString());
        final Class<? extends Exception> expected = status > 0 && status < 500 ?
                DashboardServiceException.class : DashboardServiceConnectionException.class;
        assertThrows(expected, () -> {
            switch (operation) {
                case "create" -> gateway.create(CreateDashboardDto.builder().build());
                case "update" -> gateway.update(DATABASE_1_DTO);
                case "content" -> gateway.updateContent(DATABASE_1_DTO);
                case "access" -> gateway.updateAccess(DATABASE_1_DTO.getDashboardUid(), USER_2_USERNAME, PermissionTypeDto.VIEW);
                case "owner" -> gateway.requireUser(USER_1_USERNAME);
                default -> throw new IllegalArgumentException(operation);
            }
        });
    }
}
