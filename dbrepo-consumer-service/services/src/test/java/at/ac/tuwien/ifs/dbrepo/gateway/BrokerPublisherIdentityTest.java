package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.config.RabbitConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class BrokerPublisherIdentityTest {
    @Test void onlyBrokerValidatedIdentityReachesDataService() throws Exception {
        DataServiceGateway gateway=mock(DataServiceGateway.class);
        RabbitConfig config=new RabbitConfig(new ObjectMapper(),gateway);
        ReflectionTestUtils.setField(config,"queueName","synthetic");
        ReflectionTestUtils.setField(config,"minConcurrent",1);
        ReflectionTestUtils.setField(config,"maxConcurrent",1);
        ReflectionTestUtils.setField(config,"requeueRejected",false);
        ReflectionTestUtils.setField(config,"connectionTimeout",100);
        var container=config.container(mock(ConnectionFactory.class));
        MessageListener listener=(MessageListener)container.getMessageListener();
        UUID database=UUID.randomUUID(),table=UUID.randomUUID();
        MessageProperties properties=new MessageProperties();
        properties.setReceivedRoutingKey("dbrepo."+database+"."+table);
        properties.setUserId("untrusted-outbound-field");
        Message message=new Message("{\"id\":1}".getBytes(StandardCharsets.UTF_8),properties);
        assertThrows(AmqpRejectAndDontRequeueException.class,()->listener.onMessage(message));
        verifyNoInteractions(gateway);
        properties.setReceivedUserId("publisher");
        listener.onMessage(message);
        verify(gateway).insertRawTuple(eq(database),eq(table),any(),eq("publisher"));
    }
}
