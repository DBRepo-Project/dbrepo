package at.tuwien.config;

import at.tuwien.test.BaseTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.images.PullPolicy;

/**
 * This class configures the MariaDB container for the integration tests.
 */
@Configuration
public class MariaDbContainerConfig {

    @Bean
    public CustomMariaDBContainer mariaDB() {
        return CustomMariaDBContainer.getInstance();
    }

    /**
     * This class represents the customized MariaDB container. It is a singleton to avoid the recreation of containers
     * which can be very time-consuming.
     */
    public static class CustomMariaDBContainer extends MariaDBContainer<CustomMariaDBContainer> {

        private static CustomMariaDBContainer instance;

        public static CustomMariaDBContainer getInstance() {
            if(instance == null) {
                instance = new CustomMariaDBContainer(BaseTest.IMAGE_1_NAME + ":" + BaseTest.IMAGE_1_VERSION);
                instance.withImagePullPolicy(PullPolicy.alwaysPull());
                instance.addFixedExposedPort(BaseTest.CONTAINER_1_PORT, BaseTest.IMAGE_1_PORT);
                instance.withUsername(BaseTest.CONTAINER_1_PRIVILEGED_USERNAME);
                instance.withPassword(BaseTest.CONTAINER_1_PRIVILEGED_PASSWORD);
                instance.withInitScript("init/users.sql");
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
        public void stop() {
            // do nothing, JVM handles shut down
        }
    }
}
