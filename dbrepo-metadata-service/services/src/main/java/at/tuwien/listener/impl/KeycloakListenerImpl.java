package at.tuwien.listener.impl;

import at.tuwien.config.KeycloakConfig;
import at.tuwien.entities.user.User;
import at.tuwien.listener.KeycloakListener;
import at.tuwien.repository.UserRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.RandomStringUtils;
import org.keycloak.admin.client.Keycloak;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Log4j2
@Component
public class KeycloakListenerImpl implements KeycloakListener {

    private final Keycloak keycloak;
    private final UserService userService;
    private final KeycloakConfig keycloakConfig;
    private final UserRepository userRepository;

    @Autowired
    public KeycloakListenerImpl(Keycloak keycloak, UserService userService, KeycloakConfig keycloakConfig,
                                UserRepository userRepository) {
        this.keycloak = keycloak;
        this.userService = userService;
        this.keycloakConfig = keycloakConfig;
        this.userRepository = userRepository;
    }

    @Override
    @Scheduled(fixedRate = 5000)
    public void syncUsers() {
        final List<String> knownUsernames = userService.findAll()
                .stream()
                .map(User::getUsername)
                .toList();
        final List<User> unknownUsers = keycloak.realm(keycloakConfig.getRealm())
                .users()
                .list()
                .stream()
                .filter(user -> !knownUsernames.contains(user.getUsername()))
                .map(user -> User.builder()
                        .id(UUID.fromString(user.firstAttribute("LDAP_ID")))
                        .keycloakId(UUID.fromString(user.getId()))
                        .username(user.getUsername())
                        .theme("light")
                        .mariadbPassword(userService.getMariaDbPassword(RandomStringUtils.randomAlphabetic(10))) /* user needs to set it later to access */
                        .language("en")
                        .firstname(user.getFirstName())
                        .lastname(user.getLastName())
                        .isInternal(false)
                        .build())
                .toList();
        if (unknownUsers.isEmpty()) {
            return;
        }
        userRepository.saveAll(unknownUsers);
        log.info("Synced {} unknown user(s) to metadata database", unknownUsers.size());
    }
}
