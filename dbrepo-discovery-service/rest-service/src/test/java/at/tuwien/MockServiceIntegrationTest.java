package at.tuwien;

import at.tuwien.config.H2Utils;
import at.tuwien.service.MockService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Log4j2
@SpringBootTest
public class MockServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private H2Utils h2Utils;

    @Autowired
    private MockService mockService;

    @BeforeEach
    public void beforeEach() {
        h2Utils.runScript("schema.sql");
    }

    @Test
    public void mock_succeeds() {

        /* test */
        final Boolean response = mockService.mock();
        assertTrue(response);
    }

}
