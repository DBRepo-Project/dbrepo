package at.tuwien.mvc;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.gateway.OrcidGateway;
import com.mchange.io.FileUtils;
import lombok.extern.log4j.Log4j2;
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

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Log4j2
@ExtendWith(SpringExtension.class)
@AutoConfigureMockMvc
@SpringBootTest
@MockAmqp
@MockOpensearch
public class IdentifierEndpointMvcTest extends BaseUnitTest {

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
        this.mockMvc.perform(get("/api/identifier/retrieve?url=http://orcid.org/" + USER_1_ORCID_UNCOMPRESSED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(content().string(FileUtils.getContentsAsString(new File("src/test/resources/json/ext_orcid_jdoe.json"))))
                .andDo(print())
                .andExpect(status().is(200));
    }

}
