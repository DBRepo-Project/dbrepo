package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.exception.ImageAlreadyExistsException;
import at.tuwien.exception.ImageInvalidException;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.exception.UserNotFoundException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Log4j2
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockOpensearch
public class ImageEndpointIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageEndpoint imageEndpoint;

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-image"})
    public void create_succeeds() throws UserNotFoundException, ImageAlreadyExistsException,
            ImageNotFoundException, ImageInvalidException {


        /* test */
        imageEndpoint.create(IMAGE_1_CREATE_DTO, USER_2_PRINCIPAL);
    }

}
