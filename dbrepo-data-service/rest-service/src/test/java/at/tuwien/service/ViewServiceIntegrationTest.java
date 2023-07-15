package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.repository.mdb.*;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class ViewServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @MockBean
    private Channel channel;

    @Autowired
    private RealmRepository realmRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ViewService viewService;

    @Container
    @Autowired
    private MariaDBContainer<?> mariaDBContainer;

    @BeforeEach
    public void beforeEach() throws SQLException {
        MariaDbConfig.dropAllDatabases(CONTAINER_1);
        MariaDbConfig.createInitDatabase(CONTAINER_1, DATABASE_1);
        /* metadata database */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1);
        databaseRepository.save(DATABASE_1);
    }

    @Test
    public void findAll_succeeds() throws QueryMalformedException {

        /* test */
        final List<ViewDto> views = viewService.findAll(DATABASE_1);
        assertEquals(1, views.size());
        final ViewDto view0 = views.get(0);
        assertEquals("hs_weather_aus", view0.getName());
        assertEquals("hs_weather_aus", view0.getInternalName());
    }

    @Test
    public void findAll_multipleDatabases_succeeds() throws QueryMalformedException, SQLException {

        /* mock */
        MariaDbConfig.execute(DATABASE_1, "CREATE DATABASE `" + DATABASE_2_INTERNALNAME + "`;");
        MariaDbConfig.execute(DATABASE_1, "CREATE VIEW  `" + DATABASE_2_INTERNALNAME + "`.`debug` AS (SELECT 1);");

        /* test */
        final List<ViewDto> views = viewService.findAll(DATABASE_1);
        assertEquals(1, views.size());
        final ViewDto view0 = views.get(0);
        assertEquals("hs_weather_aus", view0.getName());
        assertEquals("hs_weather_aus", view0.getInternalName());
    }

    @Test
    public void find_succeeds() throws ViewNotFoundException, ColumnTypeMalformedException {

        /* test */
        final ViewDto view = viewService.find(DATABASE_1, "hs_weather_aus");
        assertEquals("hs_weather_aus", view.getInternalName());
        assertEquals(DATABASE_1_CREATOR.getId(), view.getCreatedBy());
        assertNotNull(view.getCreator());
        final List<ColumnDto> columns = view.getColumns();
        assertEquals(4, columns.size());
        final ColumnDto column0 = columns.get(0);
        assertEquals("id", column0.getInternalName());
        assertEquals(ColumnTypeDto.BIGINT, column0.getColumnType());
        final ColumnDto column1 = columns.get(1);
        assertEquals("inserted_at", column1.getInternalName());
        assertEquals(ColumnTypeDto.TIMESTAMP, column1.getColumnType());
        final ColumnDto column2 = columns.get(2);
        assertEquals("deleted_at", column2.getInternalName());
        assertEquals(ColumnTypeDto.TIMESTAMP, column2.getColumnType());
        final ColumnDto column3 = columns.get(3);
        assertEquals("total", column3.getInternalName());
        assertEquals(ColumnTypeDto.BIGINT, column3.getColumnType());
    }

    @Test
    public void find_full_succeeds() throws ViewNotFoundException, ColumnTypeMalformedException, SQLException {

        /* mock */
        MariaDbConfig.execute(DATABASE_1, "CREATE TABLE `full_example` (col1 char(20), col2 varchar(20), col3 binary(20), col4 varbinary(20), col5 tinyblob, col6 tinytext, col7 text, col8 blob(2000), col9 mediumtext, col10 mediumblob, col11 longtext, col12 longblob, col13 enum('enum1','enum2'), col14 set('set1','set2'), col15 bit(20), col16 tinyint(20), col17 bool, col18 boolean, col19 smallint(20), col20 mediumint(20), col21 int(20), col22 integer(20), col23 bigint(20), col24 float(20), col25 float(40), col26 double(20,5), col27 double precision(20,5), col28 decimal(20,5), col29 dec(20,5), col30 date, col31 datetime, col32 timestamp, col33 time, col34 year);");
        MariaDbConfig.execute(DATABASE_1, "CREATE VIEW `full_view_example` AS (SELECT * FROM `full_example`);");

        /* test */
        final ViewDto view = viewService.find(DATABASE_1, "full_view_example");
        assertEquals("full_view_example", view.getName());
        assertEquals("full_view_example", view.getInternalName());
        assertEquals(DATABASE_1_CREATOR.getId(), view.getCreatedBy());
        assertNotNull(view.getCreator());
        final List<ColumnDto> columns = view.getColumns();
        assertEquals(34, columns.size());
        final List<ColumnTypeDto> types = List.of(ColumnTypeDto.CHAR, ColumnTypeDto.VARCHAR, ColumnTypeDto.BINARY, ColumnTypeDto.VARBINARY, ColumnTypeDto.TINYBLOB, ColumnTypeDto.TINYTEXT, ColumnTypeDto.TEXT, ColumnTypeDto.BLOB, ColumnTypeDto.MEDIUMTEXT, ColumnTypeDto.MEDIUMBLOB, ColumnTypeDto.LONGTEXT, ColumnTypeDto.LONGBLOB, ColumnTypeDto.ENUM, ColumnTypeDto.SET, ColumnTypeDto.BIT, ColumnTypeDto.TINYINT, ColumnTypeDto.BOOL, ColumnTypeDto.BOOL, ColumnTypeDto.SMALLINT, ColumnTypeDto.MEDIUMINT, ColumnTypeDto.INT, ColumnTypeDto.INT, ColumnTypeDto.BIGINT, ColumnTypeDto.FLOAT, ColumnTypeDto.DOUBLE, ColumnTypeDto.DOUBLE, ColumnTypeDto.DOUBLE, ColumnTypeDto.DECIMAL, ColumnTypeDto.DECIMAL, ColumnTypeDto.DATE, ColumnTypeDto.DATETIME, ColumnTypeDto.TIMESTAMP, ColumnTypeDto.TIME, ColumnTypeDto.YEAR);
        for (int i = 0; i < columns.size(); i++) {
            final ColumnDto column = columns.get(i);
            assertEquals("col" + (i + 1), column.getName());
            assertEquals("col" + (i + 1), column.getInternalName());
            log.trace("column {} has type {}", column.getInternalName(), column.getColumnType());
            assertEquals(types.get(i), column.getColumnType());
        }
    }

    @Test
    public void find_notFound_fails() {

        /* test */
        assertThrows(ViewNotFoundException.class, () -> {
            viewService.find(DATABASE_1, "i_d0_n0t_3x1st");
        });
    }

    @Test
    public void save_succeeds() throws ViewNameExistsException {

        /* test */
        final View response = viewService.save(VIEW_1_DTO);
        assertEquals(VIEW_1_INTERNAL_NAME, response.getName());
        assertEquals(VIEW_1_INTERNAL_NAME, response.getInternalName());
        final List<TableColumn> columns = response.getColumns();
        assertEquals(3, columns.size());
    }
}
