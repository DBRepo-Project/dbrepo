package at.ac.tuwien.ac.at.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.images.PullPolicy;

/**
 * This class configures the MariaDB container for the integration tests.
 */
@Log4j2
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
                instance.withImagePullPolicy(PullPolicy.alwaysPull());
                instance.addFixedExposedPort(BaseTest.CONTAINER_1_PORT, IMAGE_1_DEFAULT_PORT);
                instance.withUsername(BaseTest.CONTAINER_1_PRIVILEGED_USERNAME);
                instance.withPassword(BaseTest.CONTAINER_1_PRIVILEGED_PASSWORD);
                instance.withInitScript("init/users.sql");
                instance.withFileSystemBind("/tmp", "/tmp");
            }
            return instance;
        }

        private CustomMariaDBContainer(String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        protected void configure() {
            super.configure();
            this.addEnv("MYSQL_USER", "test"); // MariaDB does not allow this to be root
        }

        @Override
        public synchronized void start() {
            if (!started) {
                super.stop();
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
