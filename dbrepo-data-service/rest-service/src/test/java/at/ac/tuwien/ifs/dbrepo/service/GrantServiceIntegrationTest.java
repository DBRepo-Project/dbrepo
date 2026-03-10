package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.PostgresContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseGrantsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.GrantTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AccessNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Testcontainers
public class GrantServiceIntegrationTest extends BaseTest {

    @Autowired
    private GrantService grantService;

    @Value("${dbrepo.grant.default.read}")
    private String grantDefaultRead;

    @Value("${dbrepo.grant.default.write}")
    private String grantDefaultWrite;

    @Container
    private static PostgresContainerConfig.CustomPostgresContainer postgresContainer = PostgresContainerConfig.getContainer();

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.dropDatabase(CONTAINER_4_CACHE, DATABASE_4_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        MariaDbUtil.createInitDatabase(DATABASE_4_CACHE);
        MariaDbUtil.revokeAccess(DATABASE_1_CACHE, USER_1_USERNAME);
        MariaDbUtil.revokeAccess(DATABASE_4_CACHE, USER_4_USERNAME);
    }

    @Test
    public void find_read_succeeds() throws SQLException, DatabaseMalformedException, AccessNotFoundException {

        /* mock */
        MariaDbUtil.grantAccess(DATABASE_1_CACHE, grantDefaultRead, USER_1_USERNAME);

        /* test */
        final DatabaseGrantsDto response = grantService.find(DATABASE_1_CACHE, USER_1_USERNAME);
        assertNotNull(response);
        assertEquals(GrantTypeDto.READ, response.getType());
        assertEquals(Arrays.stream(grantDefaultRead.split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet()), response.getGrants());
    }

    @Test
    public void find_read2_succeeds() throws SQLException, DatabaseMalformedException, AccessNotFoundException {

        /* mock */
        MariaDbUtil.grantAccess(DATABASE_4_CACHE, grantDefaultRead, USER_4_USERNAME);

        /* test */
        final DatabaseGrantsDto response = grantService.find(DATABASE_4_CACHE, USER_4_USERNAME);
        assertNotNull(response);
        assertEquals(GrantTypeDto.READ, response.getType());
        assertEquals(Arrays.stream(grantDefaultRead.split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet()), response.getGrants());
    }

    @Test
    public void find_write_succeeds() throws SQLException, DatabaseMalformedException, AccessNotFoundException {

        /* mock */
        MariaDbUtil.grantAccess(DATABASE_1_CACHE, grantDefaultWrite, USER_1_USERNAME);

        /* test */
        final DatabaseGrantsDto response = grantService.find(DATABASE_1_CACHE, USER_1_USERNAME);
        assertNotNull(response);
        assertEquals(GrantTypeDto.WRITE, response.getType());
        assertEquals(Arrays.stream(grantDefaultWrite.split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet()), response.getGrants());
    }

    @Test
    public void findAll_succeeds() throws SQLException, DatabaseMalformedException {

        /* mock */
        MariaDbUtil.grantAccess(DATABASE_1_CACHE, grantDefaultRead, USER_1_USERNAME);

        /* test */
        final Map<String, DatabaseGrantsDto> response = grantService.findAll(DATABASE_1_CACHE, USER_1_USERNAME);
        assertNotNull(response);
        assertEquals(2, response.size());
        final DatabaseGrantsDto grants0 = response.get("*");
        assertEquals(Collections.EMPTY_SET, grants0.getGrants());
        assertNull(grants0.getType());
        final DatabaseGrantsDto grants1 = response.get(DATABASE_1_INTERNAL_NAME);
        assertEquals(Arrays.stream(grantDefaultRead.split(",")).map(String::trim).map(String::toUpperCase).collect(Collectors.toSet()), grants1.getGrants());
        assertEquals(GrantTypeDto.READ, grants1.getType());
    }

}
