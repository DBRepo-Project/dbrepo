
package at.tuwien.mapper;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.entities.container.Container;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class ContainerMapperTest extends BaseUnitTest {

    @Test
    public void equals_fails() {

        /* test */
        assertNotEquals(CONTAINER_1, CONTAINER_2);
    }

    @Test
    public void equals_identity_succeeds() {

        /* test */
        assertEquals(CONTAINER_1, CONTAINER_1);
    }

    @Test
    public void equals_similar_succeeds() {
        final Container tmp = Container.builder()
                .id(CONTAINER_1_ID)
                .build();

        /* test */
        assertEquals(CONTAINER_1, tmp);
    }

}
