package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.repository.sdb.UserIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
public class SwaggerComponentTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private UserIdxRepository userIdxRepository;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Channel channel;

    @Test
    public void swaggerUi_succeeds() throws Exception {
        this.mockMvc.perform(get("/swagger-ui/index.html"))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
