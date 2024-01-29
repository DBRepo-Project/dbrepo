package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.api.orcid.OrcidDto;
import at.tuwien.api.ror.RorDto;
import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.api.user.external.affiliation.ExternalAffiliationDto;
import at.tuwien.exception.DoiNotFoundException;
import at.tuwien.exception.OrcidNotFoundException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.RorNotFoundException;
import at.tuwien.gateway.CrossrefGateway;
import at.tuwien.gateway.OrcidGateway;
import at.tuwien.gateway.RorGateway;
import at.tuwien.repository.mdb.IdentifierRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockListeners
@MockOpensearch
public class MetadataServiceUnitTest extends BaseUnitTest {

    @MockBean
    private IdentifierRepository identifierRepository;

    @MockBean
    private OrcidGateway orcidGateway;

    @MockBean
    private RorGateway rorGateway;

    @MockBean
    private CrossrefGateway crossrefGateway;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void findByUrl_orcid_succeeds() throws OrcidNotFoundException, RemoteUnavailableException,
            RorNotFoundException, IOException, DoiNotFoundException {
        final OrcidDto orcid = objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(new File("src/test/resources/json/orcid_jdoe.json"), OrcidDto.class);

        /* mock */
        when(orcidGateway.findByUrl(USER_1_ORCID_URL))
                .thenReturn(orcid);

        /* test */
        final ExternalMetadataDto response = metadataService.findByUrl(USER_1_ORCID_URL);
        assertEquals(USER_1_FIRSTNAME, response.getGivenNames());
        assertEquals(USER_1_LASTNAME, response.getFamilyName());
    }

    @Test
    public void findByUrl_orcid_fails() throws OrcidNotFoundException {

        /* mock */
        doThrow(OrcidNotFoundException.class)
                .when(orcidGateway)
                .findByUrl(anyString());

        /* test */
        assertThrows(OrcidNotFoundException.class, () -> {
            metadataService.findByUrl("https://orcid.org/1234567890");
        });
    }

    @Test
    public void findByUrl_doi_succeeds() throws OrcidNotFoundException, RemoteUnavailableException,
            RorNotFoundException, IOException, DoiNotFoundException {
        final CrossrefDto doi = objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(new File("src/test/resources/json/doi_ec.json"), CrossrefDto.class);

        /* mock */
        when(crossrefGateway.findById(FUNDER_1_IDENTIFIER_ID_ONLY))
                .thenReturn(doi);

        /* test */
        final ExternalMetadataDto response = metadataService.findByUrl(FUNDER_1_IDENTIFIER);
        assertEquals(1, response.getAffiliations().length);
        final ExternalAffiliationDto affiliation0 = response.getAffiliations()[0];
        assertEquals(FUNDER_1_NAME, affiliation0.getOrganizationName());
        assertEquals(FUNDER_1_IDENTIFIER, affiliation0.getCrossrefFunderId());
    }

    @Test
    public void findByUrl_doi_fails() throws DoiNotFoundException {

        /* mock */
        doThrow(DoiNotFoundException.class)
                .when(crossrefGateway)
                .findById(anyString());

        /* test */
        assertThrows(DoiNotFoundException.class, () -> {
            metadataService.findByUrl("https://doi.org/10.12345/1234567890");
        });
    }

    @Test
    public void findByUrl_ror_succeeds() throws OrcidNotFoundException, RemoteUnavailableException,
            RorNotFoundException, IOException, DoiNotFoundException {
        final RorDto ror = objectMapper
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(new File("src/test/resources/json/ror_tuw.json"), RorDto.class);

        /* mock */
        when(rorGateway.findById(anyString()))
                .thenReturn(ror);

        /* test */
        final ExternalMetadataDto response = metadataService.findByUrl(CREATOR_4_AFFIL_ROR);
        assertEquals(1, response.getAffiliations().length);
        final ExternalAffiliationDto affiliation0 = Arrays.asList(response.getAffiliations()).get(0);
        assertEquals("TU Wien", affiliation0.getOrganizationName());
    }

    @Test
    public void findByUrl_ror_fails() throws RorNotFoundException {

        /* mock */
        doThrow(RorNotFoundException.class)
                .when(rorGateway)
                .findById(anyString());

        /* test */
        assertThrows(RorNotFoundException.class, () -> {
            metadataService.findByUrl("https://ror.org/1234567890");
        });
    }

    @Test
    public void findByUrl_rorMalformed_fails() {

        /* test */
        assertThrows(RorNotFoundException.class, () -> {
            metadataService.findByUrl("https://ror.org/");
        });
    }

    @Test
    public void findByUrl_isniMalformed_fails() {

        /* test */
        assertThrows(RemoteUnavailableException.class, () -> {
            metadataService.findByUrl("https://isni.org/isni/0000000506791090");
        });
    }
}
