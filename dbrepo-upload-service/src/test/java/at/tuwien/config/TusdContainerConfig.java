package at.tuwien.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.PullPolicy;

import java.io.File;

@Configuration
public class TusdContainerConfig {

    public static TusdContainer getContainer() {
        return TusdContainer.getInstance();
    }

    @Bean
    public TusdContainer tusdContainer() {
        return getContainer();
    }

    /**
     * This class represents the customized MariaDB container. It is a singleton to avoid the recreation of containers
     * which can be very time-consuming.
     */
    public static class TusdContainer extends GenericContainer<TusdContainer> {

        private static TusdContainer instance;

        private boolean started = false;

        public static synchronized TusdContainer getInstance() {
            final File filePath = new File("pre-create.sh");
            if (instance == null) {
                instance = new TusdContainer("tusproject/tusd:v2.4.0");
                instance.withFileSystemBind(filePath.getAbsolutePath(), "/srv/tusd-hooks/pre-create", BindMode.READ_ONLY);
                instance.withExposedPorts(8080);
                instance.withCommand("-max-size=2000000000",
                        "-base-path=/api/upload/files/",
                        "-hooks-dir=/srv/tusd-hooks/");
                instance.withImagePullPolicy(PullPolicy.alwaysPull());
            }
            return instance;
        }

        private TusdContainer(String dockerImageName) {
            super(dockerImageName);
        }

        @Override
        protected void configure() {
            super.configure();
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
