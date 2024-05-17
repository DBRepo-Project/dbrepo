package at.tuwien.endpoints;

import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
public class ActuatorComponentTest extends AbstractUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void actuatorInfo_succeeds() throws Exception {
        this.mockMvc.perform(get("/actuator/info"))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void actuatorLiveness_succeeds() throws Exception {
        this.mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void actuatorReadiness_succeeds() throws Exception {
        this.mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    public void actuatorPrometheus_succeeds() throws Exception {
        this.mockMvc.perform(get("/actuator/prometheus"));
    }

}
