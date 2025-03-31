package at.ac.tuwien.ifs.dbrepo.core.entity;

import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
public class EntitiesUnitTest extends BaseTest {

    @Test
    public void uuidVersion_succeeds() {

        /* test */
        assertEquals(4, UUID.randomUUID().version());
    }
}
