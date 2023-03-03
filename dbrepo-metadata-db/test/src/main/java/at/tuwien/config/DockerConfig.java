package at.tuwien.config;

import at.tuwien.entities.container.Container;
import at.tuwien.test.BaseTest;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.CreateContainerCmd;
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
public class DockerConfig extends BaseTest {

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
        final boolean hasHealthCheck = container.getHealthCheck() != null;
        do {
            final InspectContainerResponse response = dockerClient.inspectContainerCmd(container.getHash())
                    .exec();
            if (hasHealthCheck && response.getState().getHealth() == null) {
                log.error("Container does not have a healthcheck response");
                throw new InterruptedException("Container does not have a healthcheck response");
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
            throw new RuntimeException("Failed to start container " + container.getHash() + " as state " + state + " is not healthy after " + i + " tries");
        }
    }

    public static void stopContainer(Container container) {
        final InspectContainerResponse inspect = dockerClient.inspectContainerCmd(container.getHash())
                .exec();
        log.trace("container {} state {}", container.getHash(), inspect.getState().getStatus());
        if (Objects.equals(inspect.getState().getStatus(), "exited")) {
            return;
        }
        log.info("container {} needs to be stopped", container.getInternalName());
        dockerClient.stopContainerCmd(container.getHash())
                .exec();
    }

    public static void createContainer(String bind, Container container, String... environment) {
        createContainer(bind, container, null, environment);
    }

    public static void createContainer(String bind, Container container, Integer port, String... environment) {
        log.trace("creating container with internalName={}, ipAddress={}, hostname={}, environment={}",
                container.getInternalName(), container.getIpAddress(), container.getInternalName(),
                environment);
        final HostConfig hostConfig1;
        final String network = (container.getInternalName().contains("userdb") ? "fda-userdb" : "fda-public");
        if (bind == null) {
            hostConfig1 = hostConfig.withNetworkMode(network)
                    .withBinds(Bind.parse("/tmp:/tmp"));
        } else {
            hostConfig1 = hostConfig.withNetworkMode(network).withBinds(Bind.parse(bind), Bind.parse("/tmp:/tmp"));
        }
        if (port != null) {
            hostConfig1.withPortBindings(PortBinding.parse(port + ":" + port));
        }
        final CreateContainerCmd cmd = dockerClient.createContainerCmd(container.getImage().getRepository() + ":" + container.getImage().getTag())
                .withHostConfig(hostConfig1)
                .withName(container.getInternalName())
                .withIpv4Address(container.getIpAddress())
                .withHostName(container.getInternalName())
                .withEnv(environment);
        if (container.getHealthCheck() != null) {
            System.out.println("==========> healthcheck is present");
            System.out.println("==========> " + container.getHealthCheck());
            cmd.withHealthcheck(container.getHealthCheck());
        }
        container.setHash(cmd.exec().getId());
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

    public static void createAllNetworks() {
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
