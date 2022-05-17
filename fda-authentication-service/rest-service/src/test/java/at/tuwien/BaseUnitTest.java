package at.tuwien;

import org.springframework.test.context.TestPropertySource;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static String USER_1_EMAIL = "john.doe@example.com";
    public final static String USER_1_USERNAME = "jdoe";
    public final static String USER_1_PASSWORD = "s3cr3t1nf0rm4t10n";

}
