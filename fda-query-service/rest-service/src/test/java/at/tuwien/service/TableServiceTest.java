package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.query.ExecuteStatementDto;
import at.tuwien.api.database.query.ImportDto;
import at.tuwien.api.database.query.QueryResultDto;
import at.tuwien.api.database.table.TableCsvDto;
import at.tuwien.config.DockerConfig;
import at.tuwien.config.MariaDbConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.repository.jpa.ImageRepository;
import at.tuwien.repository.jpa.TableColumnRepository;
import at.tuwien.repository.jpa.TableRepository;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.exception.NotModifiedException;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Network;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.JSQLParserException;
import org.junit.Rule;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PersistenceException;
import java.io.File;
import java.math.BigInteger;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static at.tuwien.config.DockerConfig.dockerClient;
import static at.tuwien.config.DockerConfig.hostConfig;
import static org.junit.jupiter.api.Assertions.*;


@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class TableServiceTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private TableService tableService;

    @Autowired
    private TableRepository tableRepository;

    @BeforeEach
    @Transactional
    public void beforeEach() {
        imageRepository.save(IMAGE_1);
        IMAGE_1.setDateFormats(List.of(IMAGE_DATE_1, IMAGE_DATE_2));
        imageRepository.save(IMAGE_1);
        databaseRepository.save(DATABASE_1);
        TABLE_1.setDatabase(DATABASE_1);
        tableRepository.save(TABLE_1);
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        tableRepository.save(TABLE_1);
    }

    @Test
    @Transactional
    public void findAll_succeeds() throws TableNotFoundException, DatabaseNotFoundException {

        /* mock */
        final List<TableColumn> response = tableService.find(CONTAINER_1_ID, DATABASE_1_ID, TABLE_1_ID)
                .getColumns();

        /* test */
        assertEquals(5, response.size());
        assertEquals("id", response.get(0).getInternalName());
        assertEquals("date", response.get(1).getInternalName());
        assertEquals("location", response.get(2).getInternalName());
        assertEquals("mintemp", response.get(3).getInternalName());
        assertEquals("rainfall", response.get(4).getInternalName());
    }

}
