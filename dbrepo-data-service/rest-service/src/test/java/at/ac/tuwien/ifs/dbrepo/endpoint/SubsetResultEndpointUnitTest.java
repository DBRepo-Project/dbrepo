package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.endpoints.SubsetResultEndpoint;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SubsetResultEndpointUnitTest {
    @Test void retiredResultTransportReturnsGoneWithoutReadingPayloads() throws Exception {
        final var mvc=MockMvcBuilders.standaloneSetup(new SubsetResultEndpoint()).build();
        final String url="/api/v1/database/a/subset/b/result";
        mvc.perform(put(url).contentType("application/json").content("{}")).andExpect(status().isGone());
        mvc.perform(put(url+"/rows/0/chunks/0").contentType("application/octet-stream").content(new byte[]{1})).andExpect(status().isGone());
        mvc.perform(post(url+"/publish")).andExpect(status().isGone());
    }
}
