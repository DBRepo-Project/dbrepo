package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.repository.mdb.ImageRepository;
import at.tuwien.service.impl.ImageServiceImpl;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@MockAmqp
@MockListeners
@MockOpensearch
public class PersistenceIntegrationTest extends BaseUnitTest {

    @Autowired
    private ImageServiceImpl imageService;

    @Autowired
    private ImageRepository imageRepository;

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        imageRepository.save(IMAGE_1);
    }

    @Test
    public void delete_notExists_fails() {

        /* test */
        assertThrows(ImageNotFoundException.class, () -> {
            imageService.delete(9999L);
        });
    }

}
