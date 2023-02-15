package at.tuwien.mapper;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.listener.impl.RabbitMqListenerImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
public class StoreMapperTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private IndexConfig indexInitializer;

    @MockBean
    private RabbitMqListenerImpl rabbitMqListener;

    @MockBean
    private RestTemplate restTemplate;

    private final DateTimeFormatter mariaDbFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.S[SS]")
            .withZone(ZoneId.of("UTC"));

    @Test
    public void mapMariaDbInstant_succeeds() {
        final String timestamp = "2023-01-08 08:49:29.0";
        final Instant compare = Instant.ofEpochSecond(1673167769);

        /* test */
        final Instant response = LocalDateTime.parse(timestamp, mariaDbFormatter)
                .atZone(ZoneId.of("UTC"))
                .toInstant();
        assertEquals(compare, response);
    }

}
