
package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.TableRepository;
import at.tuwien.utils.AmqpUtils;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Network;
import com.github.dockerjava.api.model.PortBinding;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.List;

import static at.tuwien.config.DockerConfig.dockerClient;
import static at.tuwien.config.DockerConfig.hostConfig;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MessageQueueServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private TableIdxRepository tableidxRepository;

    @MockBean
    private TableColumnIdxRepository tableColumnidxRepository;

    @MockBean
    private TableRepository tableRepository;

    @Autowired
    private AmqpUtils amqpUtils;

    @Autowired
    private MessageQueueService messageQueueService;

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        afterAll();
        /* create network */
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
        /* create amqp */
        final CreateContainerResponse request = dockerClient.createContainerCmd(BROKER_IMAGE + ":" + BROKER_TAG)
                .withHostConfig(
                        hostConfig
                                .withNetworkMode("fda-public")
                                .withPortBindings(PortBinding.parse("15672:15672"), PortBinding.parse("5672:5672"))
                )
                .withName(BROKER_NAME)
                .withIpv4Address(BROKER_IP)
                .withHostName(BROKER_HOSTNAME)
                .withEnv("RABBITMQ_DEFAULT_USER=fda", "RABBITMQ_DEFAULT_PASS=fda")
                .exec();
        dockerClient.startContainerCmd(request.getId())
                .exec();
        Thread.sleep(12 * 1000);
    }

    @AfterAll
    public static void afterAll() {
        /* stop containers and remove them */
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
        /* remove networks */
        dockerClient.listNetworksCmd()
                .exec()
                .stream()
                .filter(n -> n.getName().startsWith("fda"))
                .forEach(network -> {
                    log.info("Delete network {}", network.getName());
                    dockerClient.removeNetworkCmd(network.getId()).exec();
                });
    }

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setDatabase(DATABASE_1);
        TABLE_2.setDatabase(DATABASE_1);
    }

    @Test
    public void init_succeeds() throws AmqpException {

        /* mock */
        when(tableRepository.findAll())
                .thenReturn(List.of(TABLE_1, TABLE_2));
        amqpUtils.createExchange(DATABASE_1_EXCHANGE);

        /* test */
        assertTrue(amqpUtils.exchangeExists(DATABASE_1_EXCHANGE));
        messageQueueService.init();
        assertTrue(amqpUtils.queueExists(TABLE_1_QUEUE_NAME));
    }

}
