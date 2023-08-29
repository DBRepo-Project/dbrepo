package at.tuwien.config;

import at.tuwien.test.BaseTest;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.images.PullPolicy;

/**
 * This class configures the MariaDB container for the integration tests.
 */
@Configuration
public class KeycloakContainerConfig {

    public static CustomKeycloakContainer getContainer() {
        return CustomKeycloakContainer.getInstance();
    }

    @Bean
    public CustomKeycloakContainer keycloakContainer() {
        return getContainer();
    }

    /**
     * This class represents the customized MariaDB container. It is a singleton to avoid the recreation of containers
     * which can be very time-consuming.
     */
    public static class CustomKeycloakContainer extends KeycloakContainer {

        private static CustomKeycloakContainer instance;

        private boolean started = false;

        public static synchronized CustomKeycloakContainer getInstance() {
            if(instance == null) {
                instance = new CustomKeycloakContainer("quay.io/keycloak/keycloak:21.0");
                instance.withImagePullPolicy(PullPolicy.alwaysPull());
                instance.addFixedExposedPort(BaseTest.CONTAINER_1_PORT, BaseTest.IMAGE_1_PORT);
                instance.withAdminUsername("fda");
                instance.withAdminPassword("fda");
                instance.addFixedExposedPort(8080, 8080);
                instance.withRealmImportFile("./dbrepo-realm.json");
            }
            return instance;
        }

        private CustomKeycloakContainer(String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        protected void configure() {
            super.configure();
            this.addEnv("KC_HOSTNAME_STRICT_HTTPS", "false");
        }

        @Override
        public synchronized void start() {
            if(!started) {
                super.start();
                started = true;
            }
        }

        @Override
        public void stop() {
            // do nothing, JVM handles shut down
        }
    }
}
