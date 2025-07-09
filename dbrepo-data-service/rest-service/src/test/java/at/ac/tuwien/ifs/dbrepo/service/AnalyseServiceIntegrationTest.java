package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.MariaDbContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.columns.ColumnTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AnalyseDataTypesException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ColumnNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class AnalyseServiceIntegrationTest extends BaseTest {

    @Autowired
    private AnalyseService analyseService;

    @Container
    private static MariaDBContainer<?> mariaDBContainer = MariaDbContainerConfig.getContainer();

    @BeforeAll
    public static void beforeAll() throws InterruptedException {
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @BeforeEach
    public void beforeEach() throws SQLException, InterruptedException {
        /* metadata database */
        MariaDbUtil.dropDatabase(CONTAINER_1_PRIVILEGED_DTO, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_PRIVILEGED_DTO);
        Thread.sleep(1000) /* wait for test container some more */;
    }

    @Test
    public void determineDataTypes_succeeds() throws DatabaseUnavailableException, ColumnNotFoundException,
            AnalyseDataTypesException {

        /* test */
        final Map<String, ColumnAnalysisResultDto> response = analyseService.determineDataTypes(DATABASE_1_PRIVILEGED_DTO, QUERY_1_DTO);
        assertEquals(5,  response.size());
        final ColumnAnalysisResultDto id = response.get("id");
        assertEquals("id", id.getName());
        assertEquals(ColumnTypeDto.BIGINT, id.getDatatype());
        final ColumnAnalysisResultDto date = response.get("date");
        assertEquals("date", date.getName());
        assertEquals(ColumnTypeDto.DATE, date.getDatatype());
        final ColumnAnalysisResultDto location = response.get("location");
        assertEquals("location", location.getName());
        assertEquals(ColumnTypeDto.VARCHAR, location.getDatatype());
        final ColumnAnalysisResultDto mintemp = response.get("mintemp");
        assertEquals("mintemp", mintemp.getName());
        assertEquals(ColumnTypeDto.DOUBLE, mintemp.getDatatype());
        final ColumnAnalysisResultDto rainfall = response.get("rainfall");
        assertEquals("rainfall", rainfall.getName());
        assertEquals(ColumnTypeDto.DOUBLE, rainfall.getDatatype());
    }

}
