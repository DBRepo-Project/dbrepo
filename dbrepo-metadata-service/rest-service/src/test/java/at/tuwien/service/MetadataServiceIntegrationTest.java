package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.oaipmh.OaiErrorType;
import at.tuwien.oaipmh.OaiListIdentifiersParameters;
import at.tuwien.oaipmh.OaiRecordParameters;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@MockAmqp
@MockListeners
@MockOpensearch
public class MetadataServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private IdentifierRepository identifierRepository;

    @Autowired
    private MetadataService metadataService;

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        VIEW_1.setColumns(VIEW_1_COLUMNS);
        VIEW_2.setColumns(VIEW_2_COLUMNS);
        VIEW_3.setColumns(VIEW_3_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
        licenseRepository.save(LICENSE_1);
        containerRepository.save(CONTAINER_1);
        DATABASE_1.setAccesses(List.of());
        databaseRepository.save(DATABASE_1);
        identifierRepository.save(IDENTIFIER_1);
    }

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
        final OaiListIdentifiersParameters parameters = new OaiListIdentifiersParameters();

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
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setIdentifier("oai:1");

        /* test */
        final String response = metadataService.getRecord(parameters);
        assertTrue(response.contains("identifier"));
        assertTrue(response.contains("datestamp"));
        assertTrue(response.contains("title"));
        assertTrue(response.contains("description"));
        assertTrue(response.contains("publisher"));
    }

    @Test
    public void getRecord_oaiNotFound_fails() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setIdentifier("oai:9999");

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            metadataService.getRecord(parameters);
        });
    }

    @Test
    public void getRecord_doiNotFound_fails() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setIdentifier("doi:10.1111/abcd-efgh");

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            metadataService.getRecord(parameters);
        });
    }

    @Test
    public void getRecord_prefixMalformed_fails() {
        final OaiRecordParameters parameters = new OaiRecordParameters();
        parameters.setIdentifier("pid:1");

        /* test */
        assertThrows(IdentifierNotFoundException.class, () -> {
            metadataService.getRecord(parameters);
        });
    }

}
