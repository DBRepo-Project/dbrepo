package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.exception.AmqpException;
import at.tuwien.gateway.BrokerServiceGateway;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.service.impl.RabbitMqServiceImpl;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class MessageQueueServiceUnitTest extends BaseUnitTest {

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private BrokerServiceGateway brokerServiceGateway;

    @MockBean
    private Channel channel;

    @Autowired
    private RabbitMqServiceImpl messageQueueService;

    @Test
    @Disabled("cannot be mocked")
    public void createExchange_fails() throws IOException {

        /* mock */
        doThrow(IOException.class)
                .when(channel)
                .exchangeDeclare(DATABASE_1_EXCHANGE, BuiltinExchangeType.DIRECT, true);

        /* test */
        assertThrows(AmqpException.class, () -> {
            messageQueueService.createExchange(DATABASE_1, USER_1_PRINCIPAL);
        });
    }

    @Test
    @Disabled("cannot be mocked")
    public void deleteExchange_fails() throws IOException {

        /* mock */
        doThrow(IOException.class)
                .when(channel)
                .exchangeDelete(DATABASE_1_EXCHANGE);

        /* test */
        assertThrows(AmqpException.class, () -> {
            messageQueueService.deleteExchange(DATABASE_1);
        });
    }

}
