package at.ac.tuwien.ifs.dbrepo.mvc;

import at.ac.tuwien.ifs.dbrepo.gateway.OrcidGateway;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import com.mchange.io.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.io.File;

import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Slf4j
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
public class IdentifierEndpointMvcTest extends BaseTest {

    @MockBean
    private OrcidGateway orcidGateway;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void createUser_malformed_fails() throws Exception {

        /* mock */
        when(orcidGateway.findByUrl(anyString()))
                .thenReturn(ORCID_1_DTO);

        /* test */
        this.mockMvc.perform(get("/api/v1/identifier/retrieve?url=" + USER_1_ORCID_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(FileUtils.getContentsAsString(new File("src/test/resources/json/ext_orcid_jdoe.json"))))
                .andDo(print())
                .andExpect(status().is(200));
    }

}
