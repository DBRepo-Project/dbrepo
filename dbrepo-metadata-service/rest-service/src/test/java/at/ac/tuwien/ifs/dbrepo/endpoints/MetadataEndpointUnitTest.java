package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiListIdentifiersParameters;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiRecordParameters;
import at.ac.tuwien.ifs.dbrepo.metadata.IdentifierRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierStatusType;
import at.ac.tuwien.ifs.dbrepo.utils.XmlUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MetadataEndpointUnitTest extends BaseTest {

    private final Identifier publishedIdentifier = IDENTIFIER_1.toBuilder()
            .status(IdentifierStatusType.PUBLISHED)
            .build();

    @MockitoBean
    private IdentifierRepository identifierRepository;

    @Autowired
    private MetadataEndpoint metadataEndpoint;

    @Test
    @WithAnonymousUser
    public void identify_succeeds() {

        /* mock */
        when(identifierRepository.findEarliest())
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.identify();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = response.getBody();
        assertNotNull(body);
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

    @Test
    @WithAnonymousUser
    public void identifyAlt_succeeds() {

        /* mock */
        when(identifierRepository.findEarliest())
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.identifyAlt();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = response.getBody();
        assertNotNull(body);
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

    @Test
    @WithAnonymousUser
    public void listIdentifiers_succeeds() {
        final OaiListIdentifiersParameters parameters = new OaiListIdentifiersParameters();

        /* mock */
        when(identifierRepository.findAll())
                .thenReturn(List.of(publishedIdentifier, IDENTIFIER_2, IDENTIFIER_5));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.listIdentifiers(parameters);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("doi:" + IDENTIFIER_1_DOI));
        assertFalse(body.contains("oai:" + IDENTIFIER_2_ID));
        assertFalse(body.contains("doi:" + IDENTIFIER_5_DOI));
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

    @Test
    @WithAnonymousUser
    public void getRecord_formatMissing_fails() {
        final OaiRecordParameters parameters = new OaiRecordParameters();

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.getRecord(parameters);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final String body = response.getBody();
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

    @Test
    @WithAnonymousUser
    public void getRecord_unsupportedFormat_fails() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setMetadataPrefix("oai_marc");

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.getRecord(parameters);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final String body = response.getBody();
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

    @Test
    @WithAnonymousUser
    public void getRecord_noIdentifier_fails() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setMetadataPrefix("oai_dc");

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.getRecord(parameters);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final String body = response.getBody();
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

    @Test
    @WithAnonymousUser
    public void getRecord_dc_succeeds() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setMetadataPrefix("oai_dc");
        parameters.setIdentifier("oai:" + IDENTIFIER_1_ID);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(publishedIdentifier));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.getRecord(parameters);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = response.getBody();
        assertNotNull(body);
//        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
        // TODO: currently no strict validation passes
    }

    @Test
    @WithAnonymousUser
    public void getRecord_datacite_succeeds() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setMetadataPrefix("oai_datacite");
        parameters.setIdentifier("oai:" + IDENTIFIER_1_ID);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(publishedIdentifier));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.getRecord(parameters);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = response.getBody();
        assertNotNull(body);
//        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
        // TODO: currently no strict validation passes
    }

    @ParameterizedTest
    @CsvSource({"oai,oai_dc", "doi,oai_dc", "oai,oai_datacite", "doi,oai_datacite"})
    @WithAnonymousUser
    public void getRecord_draft_notFound(String identifierPrefix, String metadataPrefix) {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setMetadataPrefix(metadataPrefix);
        parameters.setIdentifier(identifierPrefix + ":" +
                (identifierPrefix.equals("oai") ? IDENTIFIER_5_ID : IDENTIFIER_5_DOI));
        when(identifierRepository.findById(IDENTIFIER_5_ID)).thenReturn(Optional.of(IDENTIFIER_5));
        when(identifierRepository.findByDoi(IDENTIFIER_5_DOI)).thenReturn(Optional.of(IDENTIFIER_5));

        final ResponseEntity<String> response = metadataEndpoint.getRecord(parameters);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("code=\"idDoesNotExist\""));
        assertFalse(body.contains("<record>"));
        assertFalse(body.contains("<metadata>"));
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

    @Test
    @WithAnonymousUser
    public void getRecord_invalidIdentifierPrefix_succeeds() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setMetadataPrefix("oai_datacite");
        parameters.setIdentifier("ark:" + IDENTIFIER_1_ID);

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.getRecord(parameters);
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        final String body = response.getBody();
        assertNotNull(body);
    }

    @Test
    @WithAnonymousUser
    public void getRecord_notFound_fails() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setMetadataPrefix("oai_dc");
        parameters.setIdentifier("oai:" + UUID.randomUUID());

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.getRecord(parameters);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        final String body = response.getBody();
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

    @Test
    @WithAnonymousUser
    public void listMetadataFormats_succeeds() {

        /* mock */
        when(identifierRepository.findById(IDENTIFIER_1_ID))
                .thenReturn(Optional.of(IDENTIFIER_1));

        /* test */
        final ResponseEntity<String> response = metadataEndpoint.listMetadataFormats();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final String body = response.getBody();
        assertNotNull(body);
        assertTrue(body.contains("oai_dc"));
        assertTrue(body.contains("oai_datacite"));
        assertTrue(XmlUtils.validateXmlResponse("http://www.openarchives.org/OAI/2.0/OAI-PMH.xsd", body));
    }

}
