package at.tuwien.mvc;

import at.tuwien.annotations.MockAmqp;
import at.tuwien.gateway.MetadataServiceGateway;
import at.tuwien.service.SubsetService;
import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@AutoConfigureObservability
@MockAmqp
public class SubsetEndpointMvcTest extends AbstractUnitTest {

    @MockBean
    private MetadataServiceGateway metadataServiceGateway;

    @MockBean
    private SubsetService subsetService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void findById_noAcceptHeader_succeeds() throws Exception {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);

        /* test */
        this.mockMvc.perform(get("/api/database/" + DATABASE_3_ID + "/subset/" + QUERY_5_ID))
                .andDo(print())
                .andExpect(status().isOk());
    }

    @Test
    public void findById_jsonAcceptHeader_succeeds() throws Exception {

        /* mock */
        when(metadataServiceGateway.getDatabaseById(DATABASE_3_ID))
                .thenReturn(DATABASE_3_PRIVILEGED_DTO);
        when(subsetService.findById(DATABASE_3_PRIVILEGED_DTO, QUERY_5_ID))
                .thenReturn(QUERY_5_DTO);

        /* test */
        this.mockMvc.perform(get("/api/database/" + DATABASE_3_ID + "/subset/" + QUERY_5_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());
    }

}
