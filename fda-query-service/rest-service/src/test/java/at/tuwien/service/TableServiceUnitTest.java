package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexInitializer;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @Autowired
    private TableService tableService;

    @MockBean
    private TableRepository tableRepository;

    @Test
    public void findAll_succeeds() throws TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(tableRepository.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID))
                .thenReturn(Optional.of(TABLE_1));

        /* test */
        final List<TableColumn> response = tableService.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID)
                .getColumns();
        assertEquals(5, response.size());
        assertEquals("id", response.get(0).getInternalName());
        assertEquals("date", response.get(1).getInternalName());
        assertEquals("location", response.get(2).getInternalName());
        assertEquals("mintemp", response.get(3).getInternalName());
        assertEquals("rainfall", response.get(4).getInternalName());
    }

}
