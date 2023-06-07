package at.tuwien.repository;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.View;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.ViewIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class ViewRepositoryIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private ViewIdxRepository viewIdxRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        tableRepository.saveAll(List.of(TABLE_1, TABLE_2, TABLE_3, TABLE_7));
        viewRepository.save(VIEW_1);
        viewRepository.save(VIEW_3);
        viewRepository.save(VIEW_3);
    }

    @Test
    public void findAllPublicByDatabaseId_succeeds() {

        /* test */
        final List<View> response = viewRepository.findAllPublicByDatabaseId(DATABASE_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void findAllPublicOrMineByDatabaseId_succeeds() {

        /* test */
        final List<View> response = viewRepository.findAllPublicOrMineByDatabaseId(DATABASE_1_ID, USER_1_USERNAME);
        assertEquals(3, response.size());
    }

}
