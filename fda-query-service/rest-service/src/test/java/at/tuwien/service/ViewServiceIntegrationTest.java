package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.ViewCreateDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.View;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static at.tuwien.config.DockerConfig.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ViewRepository viewRepository;

    @Autowired
    private ViewService viewService;

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
        /* create container */
        final String bind = new File("./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse response = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb").withBinds(Bind.parse(bind)))
                .withName(CONTAINER_1_INTERNALNAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withHealthcheck(CONTAINER_1_HEALTHCHECK)
                .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb", "MARIADB_DATABASE=weather")
                .exec();
        /* start */
        CONTAINER_1.setHash(response.getId());
        startContainer(CONTAINER_1);
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

    @Test
    public void create_viewJoinOnView_succeeds() throws DatabaseNotFoundException, UserNotFoundException,
            DatabaseConnectionException, ViewMalformedException, QueryMalformedException, SQLException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_3_NAME)
                .query(VIEW_3_QUERY)
                .isPublic(VIEW_3_PUBLIC)
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(viewRepository.save(any(View.class)))
                .thenReturn(VIEW_3);

        /* test */
        final View response = viewService.create(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals(VIEW_3_ID, response.getId());
        assertEquals(VIEW_3_NAME, response.getName());
        assertEquals(VIEW_3_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_3_QUERY, response.getQuery());
        final List<Map<String, String>> resultSet = MariaDbConfig.selectQuery(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME,
                "SELECT j.* FROM `junit3` j", "mintemp", "rainfall", "location", "lat", "lng");
        assertEquals("13.4", resultSet.get(0).get("mintemp"));
        assertEquals("0.6", resultSet.get(0).get("rainfall"));
        assertEquals("Albury", resultSet.get(0).get("location"));
        assertEquals("-36.0653583", resultSet.get(0).get("lat"));
        assertEquals("146.9112214", resultSet.get(0).get("lng"));
        assertEquals("7.4", resultSet.get(1).get("mintemp"));
        assertEquals("0", resultSet.get(1).get("rainfall"));
        assertEquals("Albury", resultSet.get(1).get("location"));
        assertEquals("-36.0653583", resultSet.get(1).get("lat"));
        assertEquals("146.9112214", resultSet.get(1).get("lng"));
        assertEquals("12.9", resultSet.get(2).get("mintemp"));
        assertEquals("0", resultSet.get(2).get("rainfall"));
        assertEquals("Albury", resultSet.get(2).get("location"));
        assertEquals("-36.0653583", resultSet.get(2).get("lat"));
        assertEquals("146.9112214", resultSet.get(2).get("lng"));
    }

    @Test
    public void create_succeeds() throws DatabaseNotFoundException, UserNotFoundException, DatabaseConnectionException,
            ViewMalformedException, QueryMalformedException, SQLException {
        final ViewCreateDto request = ViewCreateDto.builder()
                .name(VIEW_1_NAME)
                .query(VIEW_1_QUERY)
                .isPublic(VIEW_1_PUBLIC)
                .build();

        /* mock */
        when(databaseRepository.findByContainerIdAndDatabaseId(CONTAINER_1_ID, DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));
        when(viewRepository.save(any(View.class)))
                .thenReturn(VIEW_1);

        /* test */
        final View response = viewService.create(CONTAINER_1_ID, DATABASE_1_ID, request, USER_1_PRINCIPAL);
        assertEquals(VIEW_1_ID, response.getId());
        assertEquals(VIEW_1_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        assertEquals(VIEW_1_QUERY, response.getQuery());
        final List<Map<String, String>> resultSet = MariaDbConfig.selectQuery(CONTAINER_1_INTERNALNAME, DATABASE_1_INTERNALNAME,
                "SELECT l.* FROM `weather_location` l", "location", "lat", "lng");
        assertEquals("Albury", resultSet.get(0).get("location"));
        assertEquals("-36.0653583", resultSet.get(0).get("lat"));
        assertEquals("146.9112214", resultSet.get(0).get("lng"));
        assertEquals("Sydney", resultSet.get(1).get("location"));
        assertEquals("-33.847927", resultSet.get(1).get("lat"));
        assertEquals("150.6517942", resultSet.get(1).get("lng"));
    }

}
