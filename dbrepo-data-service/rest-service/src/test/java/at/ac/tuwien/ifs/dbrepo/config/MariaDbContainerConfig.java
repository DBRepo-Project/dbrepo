package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

/**
 * This class configures the MariaDB container for the integration tests.
 */
@Slf4j
@Configuration
public class MariaDbContainerConfig extends BaseTest {

    public static CustomMariaDBContainer getContainer() {
        return CustomMariaDBContainer.getInstance();
    }

    @Bean
    public CustomMariaDBContainer mariaDB() {
        return getContainer();
    }

    /**
     * This class represents the customized MariaDB container. It is a singleton to avoid the recreation of containers
     * which can be very time-consuming.
     */
    public static class CustomMariaDBContainer extends MariaDBContainer<CustomMariaDBContainer> {

        private static CustomMariaDBContainer instance;

        private boolean started = false;

        public static synchronized CustomMariaDBContainer getInstance() {
            if (instance == null) {
                instance = new CustomMariaDBContainer(MARIADB_IMAGE);
                instance.withImagePullPolicy(PullPolicy.defaultPolicy());
                instance.addFixedExposedPort(BaseTest.CONTAINER_1_PORT, IMAGE_1_DEFAULT_PORT);
                instance.withUsername(BaseTest.CONTAINER_1_PRIVILEGED_USERNAME);
                instance.withPassword(BaseTest.CONTAINER_1_PRIVILEGED_PASSWORD);
                instance.withInitScript("init/users.sql");
                instance.withFileSystemBind("/tmp", "/tmp");
            }
            return instance;
        }

        private CustomMariaDBContainer(String dockerImageName) {
            super(DockerImageName.parse(dockerImageName).asCompatibleSubstituteFor("mariadb"));
        }

        @Override
        protected void configure() {
            super.configure();
            this.addEnv("MARIADB_EXTRA_FLAGS", "--max_connections=20 --max-statement-time=10");
            if (this.getPassword() != null && !this.getPassword().isEmpty()) {
                this.addEnv("MARIADB_ROOT_PASSWORD", this.getPassword());
            } else {
                if (!"root".equalsIgnoreCase(this.getUsername())) {
                    throw new ContainerLaunchException("Empty password can be used only with the root user");
                }
                this.addEnv("MARIADB_ALLOW_EMPTY_PASSWORD", "yes");
            }
            this.setStartupAttempts(3);
        }

        @Override
        public synchronized void start() {
            if (!started) {
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
