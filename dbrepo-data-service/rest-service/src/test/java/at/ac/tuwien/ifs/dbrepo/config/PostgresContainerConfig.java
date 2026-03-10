package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

/**
 * This class configures the MariaDB container for the integration tests.
 */
@Slf4j
@Configuration
public class PostgresContainerConfig extends BaseTest {

    public static CustomPostgresContainer getContainer() {
        return CustomPostgresContainer.getInstance();
    }

    @Bean
    public CustomPostgresContainer mariaDB() {
        return getContainer();
    }

    /**
     * This class represents the customized MariaDB container. It is a singleton to avoid the recreation of containers
     * which can be very time-consuming.
     */
    public static class CustomPostgresContainer extends PostgreSQLContainer<CustomPostgresContainer> {

        private static CustomPostgresContainer instance;

        private boolean started = false;

        public static synchronized CustomPostgresContainer getInstance() {
            if (instance == null) {
                instance = new CustomPostgresContainer(MARIADB_IMAGE);
                instance.withImagePullPolicy(PullPolicy.defaultPolicy());
                instance.addFixedExposedPort(BaseTest.CONTAINER_1_PORT, IMAGE_1_DEFAULT_PORT);
                instance.withUsername(BaseTest.CONTAINER_1_PRIVILEGED_USERNAME);
                instance.withPassword(BaseTest.CONTAINER_1_PRIVILEGED_PASSWORD);
                instance.withInitScript("init/users.sql");
                instance.withFileSystemBind("/tmp", "/tmp");
            }
            return instance;
        }

        private CustomPostgresContainer(String dockerImageName) {
            super(DockerImageName.parse(dockerImageName).asCompatibleSubstituteFor("mariadb"));
        }

        @Override
        protected void configure() {
            super.configure();
            this.addEnv("POSTGRES_PASSWORD", this.getPassword());
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
