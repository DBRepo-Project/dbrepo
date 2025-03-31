package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.crossref.CrossrefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.OrcidDto;
import at.ac.tuwien.ifs.dbrepo.core.api.ror.RorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.ExternalMetadataDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.affiliation.ExternalAffiliationDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.CrossrefGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.OrcidGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.RorGateway;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiErrorType;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiListIdentifiersParameters;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiRecordParameters;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MetadataServiceUnitTest extends BaseTest {

    @MockBean
    private OrcidGateway orcidGateway;

    @MockBean
    private RorGateway rorGateway;

    @MockBean
    private CrossrefGateway crossrefGateway;

    @MockBean
    private IdentifierService identifierService;

    @Autowired
    private MetadataService metadataService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void identify_succeeds() {

        /* test */
        final String response = metadataService.identify();
        assertTrue(response.contains("repositoryName"));
        assertTrue(response.contains("baseURL"));
        assertTrue(response.contains("adminEmail"));
        assertTrue(response.contains("earliestDatestamp"));
        assertTrue(response.contains("deletedRecord"));
        assertTrue(response.contains("granularity"));
    }

    @Test
    public void listIdentifiers_succeeds() {
        final OaiListIdentifiersParameters parameters = OaiListIdentifiersParameters.builder()
                .build();

        when(identifierService.findAll())
                .thenReturn(List.of(IDENTIFIER_1));

        /* test */
        final String response = metadataService.listIdentifiers(parameters);
        assertTrue(response.contains("identifier"));
        assertTrue(response.contains("datestamp"));
    }

    @Test
    public void listMetadataFormats_succeeds() {

        /* test */
        final String response = metadataService.listMetadataFormats();
        assertTrue(response.contains("metadataPrefix"));
        assertTrue(response.contains("schema"));
        assertTrue(response.contains("metadataNamespace"));
    }

    @Test
    public void error_succeeds() {

        /* test */
        final String response = metadataService.error(OaiErrorType.CANNOT_DISSEMINATE_FORMAT);
        assertTrue(response.contains("error"));
    }

    @Test
    @Transactional
    public void getRecord_succeeds() throws IdentifierNotFoundException {
        final OaiRecordParameters parameters = OaiRecordParameters.builder()
                .identifier("oai:" + IDENTIFIER_1_ID)
                .build();

        /* mock */
        when(identifierService.find(IDENTIFIER_1_ID))
                .thenReturn(IDENTIFIER_1);

        /* test */
        final String response = metadataService.getRecord(parameters);
        assertTrue(response.contains("identifier"));
        assertTrue(response.contains("datestamp"));
        assertTrue(response.contains("title"));
        assertTrue(response.contains("description"));
        assertTrue(response.contains("publisher"));
    }

    @Test
    public void getRecord_oaiNotFound_fails() throws IdentifierNotFoundException {
        final OaiRecordParameters parameters = OaiRecordParameters.builder()
                .identifier("oai:deadbeef-bf9c-4943-a30a-ee5295f5b8c2")
                .build();

        /* mock */
        doThrow(IdentifierNotFoundException.class)
                .when(identifierService)
                .find(any(UUID.class));

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            metadataService.getRecord(parameters);
        });
    }

    @Test
    public void getRecord_doiNotFound_fails() throws IdentifierNotFoundException {
        final OaiRecordParameters parameters = OaiRecordParameters.builder()
                .identifier("doi:10.1111/abcd-efgh")
                .build();

        /* mock */
        doThrow(IdentifierNotFoundException.class)
                .when(identifierService)
                .findByDoi(anyString());

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            metadataService.getRecord(parameters);
        });
    }

    @Test
    public void getRecord_prefixMalformed_fails() {
        final OaiRecordParameters parameters = OaiRecordParameters.builder()
                .identifier("pid:1")
                .build();

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            metadataService.getRecord(parameters);
        });
    }

    @Test
    public void findByUrl_orcid_succeeds() throws OrcidNotFoundException, RorNotFoundException, IOException,
            DoiNotFoundException, IdentifierNotSupportedException {
        final OrcidDto orcid = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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
    public void findByUrl_doi_succeeds() throws OrcidNotFoundException, RorNotFoundException, IOException,
            DoiNotFoundException, IdentifierNotSupportedException {
        final CrossrefDto doi = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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
    public void findByUrl_ror_succeeds() throws OrcidNotFoundException, RorNotFoundException, IOException,
            DoiNotFoundException, IdentifierNotSupportedException {
        final RorDto ror = objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
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
        assertThrows(IdentifierNotSupportedException.class, () -> {
            metadataService.findByUrl("https://isni.org/isni/0000000506791090");
        });
    }
}
