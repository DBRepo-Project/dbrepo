package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.*;
import at.tuwien.repository.elastic.TableColumnIdxRepository;
import at.tuwien.repository.elastic.TableIdxRepository;
import at.tuwien.repository.jpa.*;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.security.Principal;
import java.util.Arrays;
import java.util.List;

import static at.tuwien.config.DockerConfig.dockerClient;
import static at.tuwien.config.DockerConfig.hostConfig;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceIntegrationTest extends BaseUnitTest {

    /**
     * RabbitMQ not required in this test
     */
    @MockBean
    private ReadyConfig readyConfig;

    /**
     * RabbitMQ not required in this test
     */
    @MockBean
    private Channel channel;

    /**
     * ElasticSearch not required in this test
     */
    @MockBean
    private IndexConfig indexInitializer;

    /**
     * ElasticSearch not required in this test
     */
    @MockBean
    private TableIdxRepository tableidxRepository;

    /**
     * ElasticSearch not required in this test
     */
    @MockBean
    private TableColumnIdxRepository tableColumnidxRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableService tableService;

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
        /* create container */
        final String bind = new File(
                "./src/test/resources/weather").toPath().toAbsolutePath() + ":/docker-entrypoint-initdb.d";
        log.trace("container bind {}", bind);
        final CreateContainerResponse response = dockerClient.createContainerCmd(IMAGE_1_REPOSITORY + ":" + IMAGE_1_TAG)
                .withHostConfig(hostConfig.withNetworkMode("fda-userdb").withBinds(Bind.parse(bind)))
                .withName(CONTAINER_1_INTERNALNAME)
                .withIpv4Address(CONTAINER_1_IP)
                .withHostName(CONTAINER_1_INTERNALNAME)
                .withEnv("MARIADB_USER=mariadb", "MARIADB_PASSWORD=mariadb", "MARIADB_ROOT_PASSWORD=mariadb",
                        "MARIADB_DATABASE=weather")
                .exec();
        /* start */
        CONTAINER_1.setHash(response.getId());
        DockerConfig.startContainer(CONTAINER_1);
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
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        containerRepository.save(CONTAINER_2);
        databaseRepository.save(DATABASE_1) /* will have 2 tables */;
        tableRepository.save(TABLE_1);
        tableRepository.save(TABLE_2);
    }

    @Test
    public void findAll_succeeds() throws DatabaseNotFoundException {

        /* test */
        final List<Table> response = tableService.findAll(CONTAINER_1_ID, DATABASE_1_ID);
        assertEquals(2, response.size());
    }

    @Test
    public void findAll_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.findAll(CONTAINER_2_ID, DATABASE_2_ID);
        });
    }

    @Test
    public void findById_succeeds() throws TableNotFoundException, DatabaseNotFoundException,
            ContainerNotFoundException {

        /* test */
        final Table response = tableService.findById(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);
        assertEquals(TABLE_1_ID, response.getId());
        assertEquals(TABLE_1_NAME, response.getName());
        assertEquals(TABLE_1_INTERNALNAME, response.getInternalName());
    }

    @Test
    public void findById_tableNotFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.findById(CONTAINER_1_ID, DATABASE_1_ID, TABLE_3_ID);
        });
    }

    @Test
    public void findById_databaseNotFound_fails() {

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            tableService.findById(CONTAINER_2_ID, DATABASE_3_ID, TABLE_3_ID);
        });
    }

    @Test
    public void findById_containerNotFound_fails() {

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            tableService.findById(CONTAINER_3_ID, DATABASE_3_ID, TABLE_3_ID);
        });
    }

    @Test
    public void create_succeeds() throws UserNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException,
            ContainerNotFoundException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_3_CREATE_DTO, principal);
    }

    @Test
    public void create_failedBefore_succeeds() throws UserNotFoundException, TableMalformedException, QueryMalformedException,
            DatabaseNotFoundException, ImageNotSupportedException, TableNameExistsException,
            ContainerNotFoundException {
        final Principal principal = new BasicUserPrincipal(USER_1_USERNAME);

        /* mock */
        when(tableidxRepository.save(any(TableDto.class)))
                .thenReturn(null);
        when(tableColumnidxRepository.saveAll(anyList()))
                .thenReturn(List.of());

        /* test */
        try {
            tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_4_INVALID_CREATE_DTO, principal);
        } catch (TableMalformedException e) {
            /* ignore */
        }
        tableService.createTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_4_CREATE_DTO, principal);
    }

    @Test
    public void delete_succeeds() throws TableMalformedException, QueryMalformedException, DatabaseNotFoundException,
            ImageNotSupportedException, ContainerNotFoundException, TableNotFoundException, DataProcessingException {

        /* mock */
        doNothing()
                .when(tableidxRepository)
                .delete(any(TableDto.class));

        /* test */
        tableService.deleteTable(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID);
    }

}
