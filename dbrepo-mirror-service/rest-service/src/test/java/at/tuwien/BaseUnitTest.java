package at.tuwien;

import at.tuwien.test.BaseTest;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest extends BaseTest {

}
