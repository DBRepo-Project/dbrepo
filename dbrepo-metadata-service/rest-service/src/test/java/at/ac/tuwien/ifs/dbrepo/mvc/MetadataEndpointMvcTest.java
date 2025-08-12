package at.ac.tuwien.ifs.dbrepo.mvc;

import at.ac.tuwien.ifs.dbrepo.config.MetadataConfig;
import at.ac.tuwien.ifs.dbrepo.repository.IdentifierRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Slf4j
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@SpringBootTest
public class MetadataEndpointMvcTest extends BaseTest {

    @MockBean
    private IdentifierRepository identifierRepository;

    @Autowired
    private MetadataConfig metadataConfig;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void identify_succeeds() throws Exception {

        /* mock */
        when(identifierRepository.findEarliest())
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        this.mockMvc.perform(get("/api/v1/oai"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//repositoryName").string(metadataConfig.getRepositoryName()))
                .andExpect(xpath("//request[@verb='Identify']").exists())
                .andExpect(xpath("//adminEmail").string(metadataConfig.getAdminEmail()))
                .andExpect(xpath("//earliestDatestamp").string(IDENTIFIER_1_CREATED.toString()))
                .andExpect(xpath("//baseURL").string(metadataConfig.getBaseUrl()))
                .andExpect(xpath("//granularity").string(metadataConfig.getGranularity()))
                .andExpect(status().isOk());
    }

    @Test
    public void identify_withVerb_succeeds() throws Exception {

        /* mock */
        when(identifierRepository.findEarliest())
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        this.mockMvc.perform(get("/api/v1/oai?verb=Identify"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='Identify']").exists())
                .andExpect(xpath("//repositoryName").string(metadataConfig.getRepositoryName()))
                .andExpect(xpath("//adminEmail").string(metadataConfig.getAdminEmail()))
                .andExpect(xpath("//earliestDatestamp").string(IDENTIFIER_1_CREATED.toString()))
                .andExpect(xpath("//baseURL").string(metadataConfig.getBaseUrl()))
                .andExpect(xpath("//granularity").string(metadataConfig.getGranularity()))
                .andExpect(status().isOk());
    }

    @Test
    public void listIdentifiers_succeeds() throws Exception {

        /* mock */
        when(identifierRepository.findAll())
                .thenReturn(List.of(IDENTIFIER_1, IDENTIFIER_2, IDENTIFIER_3, IDENTIFIER_4, IDENTIFIER_5));

        /* test */
        this.mockMvc.perform(get("/api/v1/oai?verb=ListIdentifiers"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='ListIdentifiers']").exists())
                .andExpect(xpath("//header[1]/identifier").string("doi:" + IDENTIFIER_1_DOI))
                .andExpect(xpath("//header[2]/identifier").string("oai:" + IDENTIFIER_2_ID))
                .andExpect(xpath("//header[3]/identifier").string("oai:" + IDENTIFIER_3_ID))
                .andExpect(xpath("//header[4]/identifier").string("oai:" + IDENTIFIER_4_ID))
                .andExpect(xpath("//header[5]/identifier").string("doi:" + IDENTIFIER_5_DOI))
                .andExpect(status().isOk());
    }

    @Test
    public void getRecord_fails() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/v1/oai?verb=GetRecord"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void getRecord_oai_succeeds() throws Exception {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        this.mockMvc.perform(get("/api/v1/oai?verb=GetRecord&identifier=oai:" + IDENTIFIER_1_ID))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='GetRecord']").exists())
                .andExpect(xpath("//request[@identifier='oai:" + IDENTIFIER_1_ID + "']").exists())
                .andExpect(xpath("//identifier").string("doi:" + IDENTIFIER_1_DOI))
                .andExpect(status().isOk());
    }

    @Test
    public void getRecord_doi_succeeds() throws Exception {

        /* mock */
        when(identifierRepository.findByDoi(IDENTIFIER_5_DOI))
                .thenReturn(Optional.of(IDENTIFIER_5));

        /* test */
        this.mockMvc.perform(get("/api/v1/oai?verb=GetRecord&identifier=doi:" + IDENTIFIER_5_DOI))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='GetRecord']").exists())
                .andExpect(xpath("//request[@identifier='doi:" + IDENTIFIER_5_DOI + "']").exists())
                .andExpect(xpath("//header/identifier").string("doi:" + IDENTIFIER_5_DOI))
                .andExpect(status().isOk());
    }

    @Test
    public void getRecord_noDoi_fails() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/v1/oai?verb=GetRecord&identifier=doi:11.1111/abcd-efgh"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void getRecord_malformed_fails() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/v1/oai?verb=GetRecord&identifier=doi:11.1111:abcd-efgh"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void listMetadataFormats_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/v1/oai?verb=ListMetadataFormats"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='ListMetadataFormats']").exists())
                .andExpect(xpath("//ListMetadataFormats/metadataFormat[1]/metadataPrefix").string("oai_dc"))
                .andExpect(xpath("//ListMetadataFormats/metadataFormat[2]/metadataPrefix").string("datacite"))
                .andExpect(xpath("//ListMetadataFormats/metadataFormat[3]/metadataPrefix").string("oai_datacite"))
                .andExpect(status().isOk());
    }

}
