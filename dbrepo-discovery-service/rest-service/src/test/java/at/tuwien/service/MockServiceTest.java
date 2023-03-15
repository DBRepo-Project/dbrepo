package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MockServiceTest extends BaseUnitTest {

    @Autowired
    private MockService mockService;

    @Test
    public void test_succeeds() {

        /* test */
        final Boolean response = mockService.mock();
        assertTrue(response);
    }

}
