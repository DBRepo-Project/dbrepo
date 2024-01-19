package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MetadataConfig;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Log4j2
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@AutoConfigureMockMvc
@SpringBootTest
@MockAmqp
@MockOpensearch
public class MetadataEndpointComponentTest extends BaseUnitTest {

    @Autowired
    private MetadataConfig metadataConfig;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2));
        licenseRepository.save(LICENSE_1);
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2));
    }

    @Test
    public void identify_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//repositoryName").string(metadataConfig.getRepositoryName()))
                .andExpect(xpath("//request[@verb='Identify']").exists())
                .andExpect(xpath("//adminEmail").string(metadataConfig.getAdminEmail()))
                .andExpect(xpath("//earliestDatestamp").string(metadataConfig.getEarliestDatestamp()))
                .andExpect(xpath("//baseURL").string(metadataConfig.getBaseUrl()))
                .andExpect(xpath("//granularity").string(metadataConfig.getGranularity()))
                .andExpect(status().isOk());
    }

    @Test
    public void identify_withVerb_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=Identify"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='Identify']").exists())
                .andExpect(xpath("//repositoryName").string(metadataConfig.getRepositoryName()))
                .andExpect(xpath("//adminEmail").string(metadataConfig.getAdminEmail()))
                .andExpect(xpath("//earliestDatestamp").string(metadataConfig.getEarliestDatestamp()))
                .andExpect(xpath("//baseURL").string(metadataConfig.getBaseUrl()))
                .andExpect(xpath("//granularity").string(metadataConfig.getGranularity()))
                .andExpect(status().isOk());
    }

    @Test
    public void listIdentifiers_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=ListIdentifiers"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='ListIdentifiers']").exists())
                .andExpect(xpath("//header[1]/identifier").string("oai:" + IDENTIFIER_1_ID))
                .andExpect(xpath("//header[2]/identifier").string("oai:" + IDENTIFIER_2_ID))
                .andExpect(xpath("//header[3]/identifier").string("oai:" + IDENTIFIER_3_ID))
                .andExpect(xpath("//header[4]/identifier").string("oai:" + IDENTIFIER_4_ID))
                .andExpect(xpath("//header[5]/identifier").string("doi:" + IDENTIFIER_5_DOI))
                .andExpect(status().isOk());
    }

    @Test
    public void getRecord_fails() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=GetRecord"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void getRecord_oai_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=GetRecord&identifier=oai:1"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='GetRecord']").exists())
                .andExpect(xpath("//request[@identifier='oai:" + IDENTIFIER_1_ID + "']").exists())
                .andExpect(xpath("//identifier").string("oai:" + IDENTIFIER_1_ID))
                .andExpect(status().isOk());
    }

    @Test
    public void getRecord_doi_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=GetRecord&identifier=doi:" + IDENTIFIER_5_DOI))
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
        this.mockMvc.perform(get("/api/oai?verb=GetRecord&identifier=doi:11.1111/abcd-efgh"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void getRecord_malformed_fails() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=GetRecord&identifier=doi:11.1111:abcd-efgh"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    public void listMetadataFormats_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=ListMetadataFormats"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='ListMetadataFormats']").exists())
                .andExpect(xpath("//ListMetadataFormats/metadataFormat[1]/metadataPrefix").string("oai_dc"))
                .andExpect(xpath("//ListMetadataFormats/metadataFormat[2]/metadataPrefix").string("oai_datacite"))
                .andExpect(status().isOk());
    }

}
