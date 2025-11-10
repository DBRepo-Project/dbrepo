package at.ac.tuwien.ifs.dbrepo.mvc;

import at.ac.tuwien.ifs.dbrepo.config.MetricsConfig;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import io.micrometer.observation.tck.TestObservationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@Import(MetricsConfig.class)
@AutoConfigureObservability
public class PrometheusEndpointMvcTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestObservationRegistry registry;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @TestConfiguration
    static class ObservationTestConfiguration {

        @Bean
        public TestObservationRegistry observationRegistry() {
            return TestObservationRegistry.create();
        }
    }

    @Test
    public void prometheus_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/actuator/prometheus"))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
