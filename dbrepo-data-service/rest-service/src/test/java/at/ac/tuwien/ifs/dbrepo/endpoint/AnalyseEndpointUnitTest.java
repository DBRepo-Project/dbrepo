package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.AnalyseEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static at.ac.tuwien.ifs.dbrepo.core.test.utils.EndpointUtils.jwtPrincipal;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AnalyseEndpointUnitTest extends BaseTest {

    @MockitoBean
    private at.ac.tuwien.ifs.dbrepo.service.StorageService storageService;

    @Autowired
    private AnalyseEndpoint analyseEndpoint;

    @MockitoBean
    private AnalyseService analyseService;

    @MockitoBean
    private MetadataService metadataService;

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"analyse-datatypes"})
    public void create_succeeds() throws DatabaseUnavailableException, StorageNotFoundException,
            AnalyseDataTypesException, ColumnNotFoundException, ImageInvalidException, RemoteUnavailableException,
            MetadataServiceException, ImageNotFoundException, StorageUnavailableException, NotAllowedException {

        /* mock */
        when(metadataService.getImage(IMAGE_1_ID))
                .thenReturn(IMAGE_1_CACHE);
        when(analyseService.determineS3CsvDataTypes(IMAGE_1_CACHE, "s3key"))
                .thenReturn(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO);

        /* test */
        final ResponseEntity<SchemaAnalysisResultDto> response = analyseEndpoint.analyseDatatypes(IMAGE_1_ID, "s3key", jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID));
        assertEquals(HttpStatus.OK, response.getStatusCode());
        final SchemaAnalysisResultDto body = response.getBody();
        assertNotNull(body);
        assertEquals(TABLE_1_COLUMNS.size(), body.getColumns().size());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getComment(), body.getComment());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getEscape(), body.getEscape());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getDelimiter(), body.getDelimiter());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getNewlineDelimiter(), body.getNewlineDelimiter());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getQuote(), body.getQuote());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getTimestampFormat(), body.getTimestampFormat());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getDateFormat(), body.getDateFormat());
        assertEquals(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO.getPrompt(), body.getPrompt());
    }


    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"analyse-datatypes"})
    void foreignUploadIsDeniedBeforeAnalysis() throws Exception {
        org.mockito.Mockito.doThrow(new StorageNotFoundException("Upload not found"))
                .when(storageService).requireUploadOwner("foreign.csv", USER_1_ID.toString());
        org.junit.jupiter.api.Assertions.assertThrows(StorageNotFoundException.class,
                () -> analyseEndpoint.analyseDatatypes(IMAGE_1_ID, "foreign.csv", jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID)));
        org.mockito.Mockito.verifyNoInteractions(metadataService, analyseService);
    }

}
