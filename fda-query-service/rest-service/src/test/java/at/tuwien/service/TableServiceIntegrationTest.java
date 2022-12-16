package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.jpa.*;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1);
        IMAGE_1.setDateFormats(List.of(IMAGE_DATE_1, IMAGE_DATE_2));
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
        TABLE_1.setDatabase(DATABASE_1);
        tableRepository.save(TABLE_1);
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        tableRepository.save(TABLE_1);
    }

    @Test
    public void findAll_succeeds() throws TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        final List<TableColumn> response = tableService.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID)
                .getColumns();

        /* test */
        assertEquals(5, response.size());
        assertEquals("id", response.get(0).getInternalName());
        assertEquals("date", response.get(1).getInternalName());
        assertEquals("location", response.get(2).getInternalName());
        assertEquals("mintemp", response.get(3).getInternalName());
        assertEquals("rainfall", response.get(4).getInternalName());
    }

}
