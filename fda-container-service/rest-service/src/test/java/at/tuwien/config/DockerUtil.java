package at.tuwien.config;

import at.tuwien.entities.container.Container;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.HostConfig;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;

@Log4j2
@Configuration
public class DockerUtil {

    private final HostConfig hostConfig;
    private final DockerClient dockerClient;

    @Autowired
    public DockerUtil(HostConfig hostConfig, DockerClient dockerClient) {
        this.hostConfig = hostConfig;
        this.dockerClient = dockerClient;
    }

    public void createContainer(Container container) {
        final CreateContainerResponse create = dockerClient.createContainerCmd(container.getImage().getRepository() + ":" + container.getImage().getTag())
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb"))
                .withName(container.getInternalName())
                .withIpv4Address(container.getIpAddress())
                .withHostName(container.getInternalName())
                .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=weather")
                .exec();
        container.setHash(create.getId());
        log.info("container {} needs to be started", container.getHash());
    }

    public void startContainer(Container container) throws InterruptedException {
        final InspectContainerResponse inspect = dockerClient.inspectContainerCmd(container.getHash())
                .exec();
        log.trace("container {} state {}", container.getHash(), inspect.getState().getStatus());
        if (Objects.equals(inspect.getState().getStatus(), "running")) {
            return;
        }
        log.trace("container {} needs to be started", container.getHash());
        dockerClient.startContainerCmd(container.getHash())
                .exec();
        Thread.sleep(12 * 1000L);
        log.info("container {} was started", container.getHash());
    }

    public void stopContainer(Container container) {
        final InspectContainerResponse inspect = dockerClient.inspectContainerCmd(container.getHash())
                .exec();
        log.trace("container {} state {}", container.getHash(), inspect.getState().getStatus());
        if (!Objects.equals(inspect.getState().getStatus(), "running")) {
            return;
        }
        log.trace("container {} needs to be stopped", container.getHash());
        dockerClient.stopContainerCmd(container.getHash())
                .exec();
        log.info("container {} was stopped", container.getHash());
    }

    public void removeContainer(Container container) {
        stopContainer(container);
        log.trace("container {} needs to be removed", container.getHash());
        dockerClient.removeContainerCmd(container.getHash())
                .exec();
        log.info("container {} was removed", container.getHash());
    }

}
