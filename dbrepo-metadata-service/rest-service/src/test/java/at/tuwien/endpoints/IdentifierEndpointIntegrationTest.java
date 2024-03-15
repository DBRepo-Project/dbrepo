package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.repository.mdb.*;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class IdentifierEndpointIntegrationTest extends BaseUnitTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseRepository databaseRepository;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ContainerRepository containerRepository;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private IdentifierEndpoint identifierEndpoint;

    @BeforeEach
    public void beforeEach() {
        TABLE_1.setColumns(TABLE_1_COLUMNS);
        TABLE_2.setColumns(TABLE_2_COLUMNS);
        TABLE_3.setColumns(TABLE_3_COLUMNS);
        TABLE_4.setColumns(TABLE_4_COLUMNS);
        TABLE_5.setColumns(TABLE_5_COLUMNS);
        TABLE_6.setColumns(TABLE_6_COLUMNS);
        TABLE_7.setColumns(TABLE_7_COLUMNS);
        TABLE_8.setColumns(TABLE_8_COLUMNS);
        /* metadata database */
        imageRepository.save(IMAGE_1);
        userRepository.saveAll(List.of(USER_1, USER_2, USER_3, USER_4));
        licenseRepository.save(LICENSE_1);
        containerRepository.saveAll(List.of(CONTAINER_1, CONTAINER_2, CONTAINER_3, CONTAINER_4));
        databaseRepository.saveAll(List.of(DATABASE_1, DATABASE_2, DATABASE_3, DATABASE_4));
    }

    @Test
    @Transactional
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_5_DTO_REQUEST, USER_4_PRINCIPAL);
        });
    }

    @Test
    @Transactional
    @WithMockUser(username = USER_1_USERNAME, authorities = {"create-identifier"})
    public void create_accessNotExists_fails() {

        /* mock */
        containerRepository.save(CONTAINER_3);
        databaseRepository.save(DATABASE_3);

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            identifierEndpoint.create(IDENTIFIER_6_DTO_REQUEST, USER_1_PRINCIPAL);
        });
    }

}
