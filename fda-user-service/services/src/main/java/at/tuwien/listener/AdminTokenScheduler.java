package at.tuwien.listener;

import at.tuwien.api.auth.TokenDto;
import at.tuwien.auth.AdminToken;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.gateway.GatewayServiceGateway;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class AdminTokenScheduler {

    private final AdminToken adminToken;
    private final GatewayServiceGateway gatewayServiceGateway;

    @Autowired
    public AdminTokenScheduler(GatewayServiceGateway gatewayServiceGateway) {
        this.gatewayServiceGateway = gatewayServiceGateway;
        this.adminToken = AdminToken.getInstance();
    }

    @Scheduled(fixedRate = 1000 * 60 * 3)
    public void retrieveAdminToken() throws RemoteUnavailableException {
        final TokenDto tokenDto = gatewayServiceGateway.getToken();
        log.trace("retrieved new admin token: {}", tokenDto);
        adminToken.setToken(tokenDto.getAccessToken());
    }

}
