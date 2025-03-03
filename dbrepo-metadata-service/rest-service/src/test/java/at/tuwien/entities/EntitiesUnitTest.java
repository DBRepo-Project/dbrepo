package at.tuwien.entities;

import at.tuwien.test.AbstractUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
public class EntitiesUnitTest extends AbstractUnitTest {

    @Test
    public void uuidVersion_succeeds() {

        /* test */
        assertEquals(4, UUID.randomUUID().version());
    }
}
