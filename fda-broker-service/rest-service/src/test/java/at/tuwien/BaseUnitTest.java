package at.tuwien;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static String USER_1_USERNAME = "junit";
    public final static String USER_1_PASSWORD = "junit";

    public final static String DATABASE_1_EXCHANGE = "sensor";

    public final static String TABLE_1_ROUTING_KEY = "sensor";

}
