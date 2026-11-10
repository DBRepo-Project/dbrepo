package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.config.GatewayConfig;
import at.ac.tuwien.ifs.dbrepo.config.JacksonConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.PermissionTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.grafana.UpdateDashboardAccessDto;
import at.ac.tuwien.ifs.dbrepo.gateway.impl.DashboardServiceGatewayImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class DashboardPermissionWireTest {
    @ParameterizedTest
    @CsvSource(value={"VIEW|View", "EDITOR|Editor", "ADMIN|Admin", "NONE|''"},delimiter='|',emptyValue="")
    void gatewaySendsThePermissionAcceptedByDashboardService(PermissionTypeDto permission,String value) throws Exception {
        GatewayConfig config=new GatewayConfig(org.mockito.Mockito.mock(at.ac.tuwien.ifs.dbrepo.service.CredentialService.class));
        ReflectionTestUtils.setField(config,"dashboardEndpoint","http://dashboard.invalid");
        ReflectionTestUtils.setField(config,"systemUsername","synthetic-system");
        ReflectionTestUtils.setField(config,"systemPassword","synthetic-password");
        var http=config.dashboardServiceRestTemplate();
        var server=MockRestServiceServer.bindTo(http).build();
        server.expect(requestTo("http://dashboard.invalid/api/v1/dashboard/fixture/access/member"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().json("{\"permission\":\""+value+"\"}"))
                .andRespond(withStatus(HttpStatus.ACCEPTED));
        new DashboardServiceGatewayImpl(http,config).updateAccess("fixture","member",permission);
        server.verify();
        var mapper=new JacksonConfig().objectMapper();
        assertEquals(permission,mapper.readValue(mapper.writeValueAsString(
                UpdateDashboardAccessDto.builder().permission(permission).build()),UpdateDashboardAccessDto.class).getPermission());
    }
}
