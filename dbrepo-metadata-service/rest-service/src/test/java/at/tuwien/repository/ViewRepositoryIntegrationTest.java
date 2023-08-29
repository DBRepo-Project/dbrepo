package at.tuwien.repository;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.entities.database.View;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class ViewRepositoryIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private TableRepository tableRepository;

    @Autowired
    private TableColumnRepository tableColumnRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ViewRepository viewRepository;

    @Autowired
    private UserRepository userRepository;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @BeforeEach
    public void beforeEach() {
        imageRepository.save(IMAGE_1);
        userRepository.save(USER_1);
        userRepository.save(USER_2);
        containerRepository.saveAll(List.of(CONTAINER_1_SIMPLE, CONTAINER_2_SIMPLE));
        databaseRepository.saveAll(List.of(DATABASE_1_SIMPLE, DATABASE_2_SIMPLE));
        tableRepository.saveAll(List.of(TABLE_1_SIMPLE, TABLE_2_SIMPLE, TABLE_3_SIMPLE, TABLE_4_SIMPLE, TABLE_5_SIMPLE, TABLE_6_SIMPLE, TABLE_7_SIMPLE));
        tableColumnRepository.saveAll(TABLE_1_COLUMNS);
        tableColumnRepository.saveAll(TABLE_2_COLUMNS);
        tableColumnRepository.saveAll(TABLE_3_COLUMNS);
        tableColumnRepository.saveAll(TABLE_4_COLUMNS);
        tableColumnRepository.saveAll(TABLE_5_COLUMNS);
        tableColumnRepository.saveAll(TABLE_6_COLUMNS);
        tableColumnRepository.saveAll(TABLE_7_COLUMNS);
        viewRepository.saveAll(List.of(VIEW_1, VIEW_2, VIEW_3, VIEW_4));
    }

    @Test
    public void findAllPublicByDatabaseId_succeeds() {

        /* test */
        final List<View> response = viewRepository.findAllPublicByDatabaseId(DATABASE_1_ID);
        assertEquals(2, response.size());
    }

    @Test
    public void findAllPublicOrMineByDatabaseId_succeeds() {

        /* test */
        final List<View> response = viewRepository.findAllPublicOrMineByDatabaseId(DATABASE_1_ID, USER_1_ID);
        assertEquals(3, response.size());
    }

}
