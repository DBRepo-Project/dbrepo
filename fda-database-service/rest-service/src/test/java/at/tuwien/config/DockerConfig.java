package at.tuwien.config;

import at.tuwien.BaseUnitTest;
import at.tuwien.entities.container.Container;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.DockerClientConfig;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import lombok.extern.log4j.Log4j2;

import java.util.Arrays;
import java.util.Objects;

@Log4j2
public class DockerConfig extends BaseUnitTest {

    private final static DockerClientConfig dockerClientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
            .withDockerHost("unix:///var/run/docker.sock")
            .build();

    private final static DockerHttpClient dockerHttpClient = new ApacheDockerHttpClient.Builder()
            .dockerHost(dockerClientConfig.getDockerHost())
            .sslConfig(dockerClientConfig.getSSLConfig())
            .build();

    public final static HostConfig hostConfig = HostConfig.newHostConfig()
            .withRestartPolicy(RestartPolicy.alwaysRestart());

    public final static DockerClient dockerClient = DockerClientBuilder.getInstance()
            .withDockerHttpClient(dockerHttpClient)
            .build();

    public static void startContainer(Container container) throws InterruptedException {
        final InspectContainerResponse inspect = dockerClient.inspectContainerCmd(container.getHash())
                .exec();
        log.trace("container {} state {}", container.getHash(), inspect.getState().getStatus());
        if (Objects.equals(inspect.getState().getStatus(), "running")) {
            return;
        }
        log.info("container {} needs to be started", container.getInternalName());
        dockerClient.startContainerCmd(container.getHash())
                .exec();
        int i = 0;
        final int max = 10;
        String state;
        final boolean hasHealthCheck = getHealthCheck(container.getId()) != null;
        do {
            final InspectContainerResponse response = dockerClient.inspectContainerCmd(container.getHash())
                    .exec();
            if (hasHealthCheck && response.getState().getHealth() == null) {
                log.error("Container does not have a healthcheck configuration");
                throw new InterruptedException("Container does not have a healthcheck configuration");
            }
            if (hasHealthCheck) {
                state = response.getState().getHealth().getStatus();
                log.trace("container {} state is {}, attempt {} of {}", container.getInternalName(), state, i, max);
                if (!state.equals("healthy")) {
                    Thread.sleep(10 * 1000L);
                }
            } else {
                Thread.sleep(60 * 1000L);
                state = "healthy";
            }
            i++;
        } while (!state.equals("healthy") && i != max);
        if (state.equals("healthy")) {
            log.info("container {} was started", container.getInternalName());
        } else {
            log.error("failed to start container {} as state {} is not healthy after {} tries", container.getHash(),
                    state, i);
            throw new RuntimeException("Failed to start container");
        }
    }

    public static void createContainer(String bind, Container container, String... environment) {
        log.trace("creating container with internalName={}, ipAddress={}, hostname={}, environment={}",
                container.getInternalName(), container.getIpAddress(), container.getInternalName(),
                environment);
        final HostConfig hostConfig1;
        final String network = (container.getInternalName().contains("userdb") ? "fda-userdb" : "fda-public");
        if (bind == null) {
            hostConfig1 = hostConfig.withNetworkMode(network);
        } else {
            hostConfig1 = hostConfig.withNetworkMode(network).withBinds(Bind.parse(bind));
        }
        final CreateContainerCmd cmd = dockerClient.createContainerCmd(container.getImage().getRepository() + ":" + container.getImage().getTag())
                .withHostConfig(hostConfig1)
                .withName(container.getInternalName())
                .withIpv4Address(container.getIpAddress())
                .withHostName(container.getInternalName())
                .withEnv(environment)
                .withHealthcheck(getHealthCheck(container.getId()));
        final CreateContainerResponse response;
        if (container.getInternalName().contains("search")) {
            response = cmd.withPortBindings(PortBinding.parse("9200:9200"))
                    .exec();
        } else {
            response = cmd.exec();
        }
        container.setHash(response.getId());
    }

    public static void stopContainer(Container container) {
        final InspectContainerResponse inspect = dockerClient.inspectContainerCmd(container.getHash())
                .exec();
        log.trace("container {} state {}", container.getHash(), inspect.getState().getStatus());
        if (!Objects.equals(inspect.getState().getStatus(), "running")) {
            return;
        }
        log.trace("container {} needs to be stopped", container.getHash());
        dockerClient.stopContainerCmd(container.getHash())
                .exec();
        log.debug("container {} was stopped", container.getHash());
    }

    public static void removeContainer(Container container) {
        final InspectContainerResponse inspect = dockerClient.inspectContainerCmd(container.getHash())
                .exec();
        log.trace("container {} state {}", container.getHash(), inspect.getState().getStatus());
        log.trace("container {} needs to be removed", container.getHash());
        dockerClient.removeContainerCmd(container.getHash())
                .exec();
        log.debug("container {} was removed", container.getHash());
    }

    private static HealthCheck getHealthCheck(Long containerId) {
        if (containerId == null) {
            log.trace("container does not have a healthcheck config");
            return null;
        }
        switch (Integer.parseInt("" + containerId)) {
            case 1:
                log.debug("container with id {} has a health check config", containerId);
                return CONTAINER_1_HEALTHCHECK;
            case 2:
                log.debug("container with id {} has a health check config", containerId);
                return CONTAINER_2_HEALTHCHECK;
            case 3:
                log.debug("container with id {} has a health check config", containerId);
                return CONTAINER_3_HEALTHCHECK;
            case 4:
                log.debug("container with id {} has a health check config", containerId);
                return CONTAINER_4_HEALTHCHECK;
            case 5:
                log.debug("container with id {} has a health check config", containerId);
                return CONTAINER_BROKER_HEALTHCHECK;
        }
        log.trace("container with id {} does not have a healthcheck config", containerId);
        return null;
    }

    public static void removeAllContainers() {
        dockerClient.listContainersCmd()
                .withShowAll(true)
                .exec()
                .forEach(container -> {
                    log.info("Delete container {}", Arrays.asList(container.getNames()));
                    try {
                        dockerClient.stopContainerCmd(container.getId()).exec();
                    } catch (NotModifiedException e) {
                        // ignore
                    }
                    dockerClient.removeContainerCmd(container.getId()).exec();
                });
        dockerClient.listVolumesCmd()
                .withDanglingFilter(true)
                .exec()
                .getVolumes()
                .forEach(volume -> {
                    log.info("Delete volume {}", volume.getName());
                    try {
                        dockerClient.removeVolumeCmd(volume.getName()).exec();
                    } catch (NotModifiedException e) {
                        // ignore
                    }
                });
    }

    public static void removeAllNetworks() {
        dockerClient.listNetworksCmd()
                .exec()
                .stream()
                .filter(n -> n.getName().startsWith("fda"))
                .forEach(network -> {
                    log.info("Delete network {}", network.getName());
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                });
    }

    public static void createAllNetworks () {
        dockerClient.createNetworkCmd()
                .withName("fda-userdb")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.28.0.0/16")))
                .withEnableIpv6(false)
                .exec();
        dockerClient.createNetworkCmd()
                .withName("fda-public")
                .withIpam(new Network.Ipam()
                        .withConfig(new Network.Ipam.Config()
                                .withSubnet("172.29.0.0/16")))
                .withEnableIpv6(false)
                .exec();
    }

}
