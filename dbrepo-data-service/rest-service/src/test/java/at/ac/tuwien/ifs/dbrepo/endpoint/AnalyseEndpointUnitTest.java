package at.ac.tuwien.ifs.dbrepo.endpoint;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.AccessTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.AccessEndpoint;
import at.ac.tuwien.ifs.dbrepo.endpoints.AnalyseEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.AccessService;
import at.ac.tuwien.ifs.dbrepo.service.AnalyseService;
import at.ac.tuwien.ifs.dbrepo.service.CacheService;
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

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AnalyseEndpointUnitTest extends BaseTest {

    @Autowired
    private AnalyseEndpoint analyseEndpoint;

    @MockitoBean
    private AnalyseService analyseService;

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"analyse-datatypes"})
    public void create_succeeds() throws DatabaseUnavailableException, StorageNotFoundException, AnalyseDataTypesException {

        /* mock */
        when(analyseService.determineDataTypes(anyString()))
                .thenReturn(TABLE_1_SCHEMA_ANALYSIS_RESULT_DTO);

        /* test */
        final ResponseEntity<SchemaAnalysisResultDto> response = analyseEndpoint.analyseDatatypes("s3key");
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

}
