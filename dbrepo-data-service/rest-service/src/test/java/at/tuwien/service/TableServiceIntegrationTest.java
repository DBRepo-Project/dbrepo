package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.api.database.table.columns.ColumnDto;
import at.tuwien.api.database.table.columns.ColumnTypeDto;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.entities.database.View;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.columns.TableColumnType;
import at.tuwien.exception.ColumnTypeMalformedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.exception.ViewNotFoundException;
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
public class TableServiceIntegrationTest extends BaseUnitTest {

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
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private TableService tableService;

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
        tableRepository.save(TABLE_1);
        tableRepository.save(TABLE_2);
        tableColumnRepository.saveAll(TABLE_1_COLUMNS);
        tableColumnRepository.saveAll(TABLE_2_COLUMNS);
    }

    @Test
    public void findAll_succeeds() throws QueryMalformedException {

        /* test */
        final List<TableBriefDto> tables = tableService.findAll(DATABASE_1);
        assertEquals(3, tables.size());
        final TableBriefDto table0 = tables.get(0);
        assertEquals("weather_aus", table0.getName());
        assertEquals("weather_aus", table0.getInternalName());
        final TableBriefDto table1 = tables.get(1);
        assertEquals("sensor", table1.getName());
        assertEquals("sensor", table1.getInternalName());
    }

    @Test
    public void findAll_multipleDatabases_succeeds() throws QueryMalformedException, SQLException {

        /* mock */
        MariaDbConfig.execute(DATABASE_1, "CREATE DATABASE `" + DATABASE_2_INTERNALNAME + "`;");
        MariaDbConfig.execute(DATABASE_1, "CREATE TABLE  `" + DATABASE_2_INTERNALNAME + "`.`debug` (`id` BIGINT PRIMARY KEY NOT NULL);");

        /* test */
        final List<TableBriefDto> tables = tableService.findAll(DATABASE_1);
        assertEquals(3, tables.size());
        final TableBriefDto table0 = tables.get(0);
        assertEquals("weather_aus", table0.getName());
        assertEquals("weather_aus", table0.getInternalName());
        final TableBriefDto table1 = tables.get(1);
        assertEquals("sensor", table1.getName());
        assertEquals("sensor", table1.getInternalName());
    }

    @Test
    public void find_succeeds() throws ColumnTypeMalformedException, TableNotFoundException {

        /* test */
        final TableDto table = tableService.find(DATABASE_1, TABLE_1_INTERNALNAME);
    }

    @Test
    public void find_full_succeeds() throws ColumnTypeMalformedException, SQLException, TableNotFoundException {

        /* mock */
        MariaDbConfig.execute(DATABASE_1, "CREATE TABLE `full_example` (col1 char(20), col2 varchar(20), col3 binary(20), col4 varbinary(20), col5 tinyblob, col6 tinytext, col7 text, col8 blob(2000), col9 mediumtext, col10 mediumblob, col11 longtext, col12 longblob, col13 enum('enum1','enum2'), col14 set('set1','set2'), col15 bit(20), col16 tinyint(20), col17 bool, col18 boolean, col19 smallint(20), col20 mediumint(20), col21 int(20), col22 integer(20), col23 bigint(20), col24 float(20), col25 float(40), col26 double(20,5), col27 double precision(20,5), col28 decimal(20,5), col29 dec(20,5), col30 date, col31 datetime, col32 timestamp, col33 time, col34 year);");
        MariaDbConfig.execute(DATABASE_1, "CREATE VIEW `full_view_example` AS (SELECT * FROM `full_example`);");

        /* test */
        final TableDto table = tableService.find(DATABASE_1, TABLE_1_INTERNALNAME);
    }

    @Test
    public void find_notFound_fails() {

        /* test */
        assertThrows(TableNotFoundException.class, () -> {
            tableService.find(DATABASE_1, "i_d0_n0t_3x1st");
        });
    }

    @Test
    public void save_succeeds() {

        /* test */
        final Table response = tableService.save(TABLE_3_DTO);
    }
}
