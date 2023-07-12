package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class OpenSearchIndexIntegrationTest extends BaseUnitTest {


}
