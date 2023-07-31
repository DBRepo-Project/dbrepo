package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import at.tuwien.repository.mdb.TableRepository;
import at.tuwien.repository.sdb.ViewIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceUnitTest extends BaseUnitTest {

    @MockBean
    private Channel channel;

    @MockBean
    private ViewIdxRepository viewIdxRepository;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @Autowired
    private TableService tableService;

    @MockBean
    private TableRepository tableRepository;

    @BeforeEach
    public void beforeEach() {
        TABLE_8.setColumns(TABLE_8_COLUMNS);
    }

    @Test
    public void findAll_succeeds() throws TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        when(tableRepository.find(DATABASE_3_ID, TABLE_8_ID))
                .thenReturn(Optional.of(TABLE_8));

        /* test */
        final List<TableColumn> response = tableService.find(DATABASE_3_ID, TABLE_8_ID)
                .getColumns();
        assertEquals(2, response.size());
        assertEquals("id", response.get(0).getInternalName());
        assertEquals("value", response.get(1).getInternalName());
    }

}
