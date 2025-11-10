package at.ac.tuwien.ifs.dbrepo.mvc;

import at.ac.tuwien.ifs.dbrepo.config.RedisContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.service.SubsetService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@AutoConfigureObservability
public class SubsetEndpointMvcTest extends BaseTest {

    @MockitoBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockitoBean
    private SubsetService subsetService;

    @Autowired
    private MockMvc mockMvc;

    @Container
    private static RedisContainerConfig.CustomRedisContainer redisContainer = RedisContainerConfig.getContainer();

    @Test
    public void findById_privateDataPublicSchema_jsonAcceptHeader_succeeds() throws Exception {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_CACHE);
        when(subsetService.findById(DATABASE_3_CACHE, QUERY_5_ID))
                .thenReturn(QUERY_5_CACHE);

        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_3_ID + "/subset/" + QUERY_5_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void findById_publicDataPublicSchema_jsonAcceptHeader_succeeds() throws Exception {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_4_ID))
                .thenReturn(DATABASE_4_CACHE);
        when(subsetService.findById(DATABASE_4_CACHE, QUERY_7_ID))
                .thenReturn(QUERY_5_CACHE);

        /* test */
        this.mockMvc.perform(get("/api/v1/database/" + DATABASE_4_ID + "/subset/" + QUERY_7_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
