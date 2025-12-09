package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.PullPolicy;
import org.testcontainers.utility.DockerImageName;

/**
 * This class configures the MariaDB container for the integration tests.
 */
@Slf4j
@Configuration
public class RedisContainerConfig extends BaseTest {

    public static CustomRedisContainer getContainer() {
        return CustomRedisContainer.getInstance();
    }

    @Bean
    public CustomRedisContainer redis() {
        return getContainer();
    }

    /**
     * This class represents the customized MariaDB container. It is a singleton to avoid the recreation of containers
     * which can be very time-consuming.
     */
    public static class CustomRedisContainer extends GenericContainer {

        private static CustomRedisContainer instance;

        private boolean started = false;

        public static synchronized CustomRedisContainer getInstance() {
            if (instance == null) {
                instance = new CustomRedisContainer(REDIS_IMAGE);
                instance.withImagePullPolicy(PullPolicy.defaultPolicy());
                instance.addFixedExposedPort(6379, 6379);
            }
            return instance;
        }

        private CustomRedisContainer(String dockerImageName) {
            super(DockerImageName.parse(dockerImageName).asCompatibleSubstituteFor("redis"));
        }

        @Override
        protected void configure() {
            super.configure();
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
            if (started) {
                super.stop();
            }
        }
    }
}
