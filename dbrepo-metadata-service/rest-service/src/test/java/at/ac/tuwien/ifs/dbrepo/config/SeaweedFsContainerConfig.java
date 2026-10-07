package at.ac.tuwien.ifs.dbrepo.config;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;

import java.time.Duration;

import static at.ac.tuwien.ifs.dbrepo.core.test.BaseTest.SEAWEEDFS_IMAGE;

/**
 * This class configures the SeaweedFS (S3) container for the integration tests.
 */
public class SeaweedFsContainerConfig {

    public static GenericContainer<?> getContainer() {
        return new GenericContainer<>(SEAWEEDFS_IMAGE)
                /* small volumes, the default preallocates 7x 1GB on startup */
                .withCommand("server", "-s3", "-s3.port=9000", "-master.volumeSizeLimitMB=64")
                .withEnv("AWS_ACCESS_KEY_ID", "seaweedfsadmin")
                .withEnv("AWS_SECRET_ACCESS_KEY", "seaweedfsadmin")
                .withExposedPorts(9000, 9333)
                /* wait for s3 and a registered volume server, don't probe /dir/assign as it triggers volume growth;
                   the first write still takes ~10s since the master only grows volumes 10s after leader election */
                .waitingFor(new WaitAllStrategy()
                        .withStrategy(Wait.forHttp("/healthz").forPort(9000))
                        .withStrategy(Wait.forHttp("/dir/status").forPort(9333)
                                .forResponsePredicate(body -> body.contains("\"DataNodes\":[{")))
                        .withStartupTimeout(Duration.ofSeconds(60)));
    }

    public static String getEndpoint(GenericContainer<?> container) {
        return "http://" + container.getHost() + ":" + container.getMappedPort(9000);
    }
}
