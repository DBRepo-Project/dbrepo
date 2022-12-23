package at.tuwien.utils;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class FileUtilTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private Channel channel;

    @Test
    public void loadResource_succeeds() throws IOException {

        /* test */
        final List<String> response = FileUtil.loadResource("/init/querystore.sql");
        assertEquals(7, response.size());
    }
}
