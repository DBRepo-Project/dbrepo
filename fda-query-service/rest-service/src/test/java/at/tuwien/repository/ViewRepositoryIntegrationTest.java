package at.tuwien.repository;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.H2Utils;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.View;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.*;
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
    private IndexConfig indexConfig;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private H2Utils h2Utils;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @BeforeEach
    public void beforeEach() {
        h2Utils.runScript("schema.sql");
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setTables(List.of());
        DATABASE_1.setViews(List.of());
        databaseRepository.save(DATABASE_1);
        viewRepository.save(VIEW_1);
        viewRepository.save(VIEW_2);
        viewRepository.save(VIEW_3);
    }

    @Test
    public void findAllPublicByDatabaseId_succeeds() {

        /* test */
        final List<View> response = viewRepository.findAllPublicByDatabaseId(DATABASE_1_ID);
        assertEquals(2, response.size());
        final View view1 = response.get(0);
        assertEquals(VIEW_1_ID, view1.getId());
        assertEquals(VIEW_1_CONTAINER_ID, view1.getVcid());
        assertEquals(VIEW_1_DATABASE_ID, view1.getVdbid());
        final View view2 = response.get(1);
        assertEquals(VIEW_2_ID, view2.getId());
        assertEquals(VIEW_2_CONTAINER_ID, view2.getVcid());
        assertEquals(VIEW_2_DATABASE_ID, view2.getVdbid());
    }

    @Test
    public void findAllPublicOrMineByDatabaseId_succeeds() {

        /* test */
        final List<View> response = viewRepository.findAllPublicOrMineByDatabaseId(DATABASE_1_ID, USER_1_USERNAME);
        assertEquals(3, response.size());
        final View view1 = response.get(0);
        assertEquals(VIEW_1_ID, view1.getId());
        assertEquals(VIEW_1_CONTAINER_ID, view1.getVcid());
        assertEquals(VIEW_1_DATABASE_ID, view1.getVdbid());
        final View view2 = response.get(1);
        assertEquals(VIEW_2_ID, view2.getId());
        assertEquals(VIEW_2_CONTAINER_ID, view2.getVcid());
        assertEquals(VIEW_2_DATABASE_ID, view2.getVdbid());
        final View view3 = response.get(2);
        assertEquals(VIEW_3_ID, view3.getId());
        assertEquals(VIEW_3_CONTAINER_ID, view3.getVcid());
        assertEquals(VIEW_3_DATABASE_ID, view3.getVdbid());
    }

}
