package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.entities.identifier.Identifier;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
public class IdentifierServiceIntegrationTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

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
    private IdentifierRepository identifierRepository;

    @Autowired
    private IdentifierService identifierService;

    @BeforeEach
    public void beforeEach() {
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_1);
        imageRepository.save(IMAGE_1);
        containerRepository.save(CONTAINER_1_SIMPLE);
        databaseRepository.save(DATABASE_1_SIMPLE);
        identifierRepository.save(IDENTIFIER_1_SIMPLE);
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_1_ID);
        assertEquals(1, response.size());
    }

    @Test
    public void find_notFound_fails() {

        /* test */
        final List<Identifier> response = identifierService.findAll(DATABASE_2_ID);
        assertEquals(0, response.size());
    }


}
