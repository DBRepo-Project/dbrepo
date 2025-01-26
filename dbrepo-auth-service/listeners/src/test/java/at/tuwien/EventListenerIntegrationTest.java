package at.tuwien;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class EventListenerIntegrationTest {

    @Container
    private static KeycloakContainer keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:24.0")
            .withImagePullPolicy(PullPolicy.alwaysPull())
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withRealmImportFile("dbrepo-realm.json")
            .withEnv("KC_HOSTNAME_STRICT_HTTPS", "false");
}
