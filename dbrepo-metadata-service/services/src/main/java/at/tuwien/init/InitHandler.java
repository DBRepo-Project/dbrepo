package at.tuwien.init;

import at.tuwien.api.keycloak.UserDto;
import at.tuwien.config.GatewayConfig;
import at.tuwien.config.MetadataConfig;
import at.tuwien.entities.user.User;
import at.tuwien.exception.AuthServiceConnectionException;
import at.tuwien.exception.AuthServiceException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.repository.UserRepository;
import at.tuwien.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Log4j2
@Component
@Profile("!junit")
public class InitHandler {

    private final UserService userService;
    private final GatewayConfig gatewayConfig;
    private final MetadataConfig metadataConfig;
    private final UserRepository userRepository;
    private final KeycloakGateway keycloakGateway;

    @Autowired
    public InitHandler(UserService userService, GatewayConfig gatewayConfig, MetadataConfig metadataConfig,
                       UserRepository userRepository, KeycloakGateway keycloakGateway) {
        this.userService = userService;
        this.gatewayConfig = gatewayConfig;
        this.metadataConfig = metadataConfig;
        this.userRepository = userRepository;
        this.keycloakGateway = keycloakGateway;
    }

    @PostConstruct
    public void init() throws UserNotFoundException, AuthServiceException, AuthServiceConnectionException {
        try {
            userService.findByUsername(gatewayConfig.getSystemUsername());
        } catch (UserNotFoundException e) {
            log.warn("Failed to find system user with username {} in metadata database", gatewayConfig.getSystemUsername());
            final UserDto user = keycloakGateway.findByUsername(gatewayConfig.getSystemUsername());
            final User entity = User.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .email(metadataConfig.getAdminEmail())
                    .theme("light")
                    .mariadbPassword(userService.getMariaDbPassword(gatewayConfig.getSystemPassword()))
                    .language("en")
                    .build();
            userRepository.save(entity);
            log.info("Saved system user with username: {}", gatewayConfig.getSystemUsername());
        }
    }
}
