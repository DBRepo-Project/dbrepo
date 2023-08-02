package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.config.MetadataConfig;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.IdentifierIdxRepository;
import at.tuwien.service.IdentifierService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Log4j2
@ExtendWith(SpringExtension.class)
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

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        imageRepository.save(IMAGE_1_SIMPLE);
        realmRepository.save(REALM_DBREPO);
        licenseRepository.save(LICENSE_1);
        userRepository.save(USER_1_SIMPLE);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_1);
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
                .andExpect(xpath("//header/identifier").string(metadataConfig.getPidBase() + IDENTIFIER_1_ID))
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
    public void getRecord_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=GetRecord&identifier=1"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='GetRecord']").exists())
                .andExpect(status().isOk());
    }

    @Test
    public void listMetadataFormats_succeeds() throws Exception {

        /* test */
        this.mockMvc.perform(get("/api/oai?verb=ListMetadataFormats"))
                .andDo(print())
                .andExpect(content().contentType("text/xml;charset=UTF-8"))
                .andExpect(xpath("//request[@verb='ListMetadataFormats']").exists())
                .andExpect(xpath("//ListMetadataFormats/metadataFormat/metadataPrefix").string("oai_dc"))
                .andExpect(status().isOk());
    }

}
