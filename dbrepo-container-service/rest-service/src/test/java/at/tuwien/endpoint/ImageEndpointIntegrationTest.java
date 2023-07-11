package at.tuwien.endpoint;

import at.tuwien.BaseUnitTest;
import at.tuwien.endpoints.ImageEndpoint;
import at.tuwien.exception.ImageAlreadyExistsException;
import at.tuwien.exception.ImageInvalidException;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import at.tuwien.repository.mdb.RealmRepository;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.repository.sdb.DatabaseIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
public class ImageEndpointIntegrationTest extends BaseUnitTest {

    @MockBean
    private DatabaseIdxRepository databaseIdxRepository;

    @MockBean
    private RealmRepository realmRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private ImageEndpoint imageEndpoint;

    @BeforeEach
    public void beforeEach() {
        /* metadata database */
        realmRepository.save(REALM_DBREPO);
        userRepository.save(USER_2_SIMPLE);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-image"})
    public void create_succeeds() throws UserNotFoundException, ImageAlreadyExistsException,
            ImageNotFoundException, ImageInvalidException {


        /* test */
        imageEndpoint.create(IMAGE_1_CREATE_DTO, USER_2_PRINCIPAL);
    }

}
