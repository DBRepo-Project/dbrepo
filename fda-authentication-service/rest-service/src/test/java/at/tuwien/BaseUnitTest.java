package at.tuwien;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@TestPropertySource(locations = "classpath:application.properties")
public abstract class BaseUnitTest {

    public final static Long USER_1_ID = 1L;
    public final static String USER_1_EMAIL = "john.doe@example.com";
    public final static String USER_1_USERNAME = "jdoe";
    public final static String USER_1_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static String USER_1_FIRSTNAME = "John";
    public final static String USER_1_LASTNAME = "Doe";
    public final static String USER_1_TITLES_BEFORE = "Dr.";
    public final static String USER_1_TITLES_AFTER = "MSc BSc";
    public final static Boolean USER_1_VERIFIED = true;
    public final static Boolean USER_1_THEME_DARK = false;
    public final static Instant USER_1_CREATED = Instant.now()
            .minus(1, ChronoUnit.DAYS);
    public final static Instant USER_1_LAST_MODIFIED = USER_1_CREATED;

    public final static User USER_1 = User.builder()
            .id(USER_1_ID)
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD)
            .firstname(USER_1_FIRSTNAME)
            .lastname(USER_1_LASTNAME)
            .titlesBefore(USER_1_TITLES_BEFORE)
            .titlesAfter(USER_1_TITLES_AFTER)
            .emailVerified(USER_1_VERIFIED)
            .themeDark(USER_1_THEME_DARK)
            .created(USER_1_CREATED)
            .lastModified(USER_1_LAST_MODIFIED)
            .build();

    public final static Long USER_2_ID = 2L;
    public final static String USER_2_EMAIL = "jane.doe@example.com";
    public final static String USER_2_USERNAME = "jdoe2";
    public final static String USER_2_PASSWORD = "s3cr3t1nf0rm4t10n";
    public final static Boolean USER_2_VERIFIED = false;
    public final static Boolean USER_2_THEME_DARK = false;
    public final static Instant USER_2_CREATED = Instant.now()
            .minus(1, ChronoUnit.DAYS);
    public final static Instant USER_2_LAST_MODIFIED = USER_1_CREATED;

    public final static User USER_2 = User.builder()
            .id(USER_2_ID)
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .password(USER_2_PASSWORD)
            .emailVerified(USER_2_VERIFIED)
            .themeDark(USER_2_THEME_DARK)
            .created(USER_2_CREATED)
            .lastModified(USER_2_LAST_MODIFIED)
            .build();

    public final static Long TOKEN_1_ID = 1L;
    public final static Boolean TOKEN_1_PROCESSED = false;
    public final static String TOKEN_1_TOKEN = "mysecrettokenrandomlygenerated";
    public final static Instant TOKEN_1_VALID_TO = Instant.now()
            .plus(1, ChronoUnit.DAYS);

    public final static Long TOKEN_2_ID = 2L;
    public final static Boolean TOKEN_2_PROCESSED = true;
    public final static String TOKEN_2_TOKEN = "blahblahblah";
    public final static Instant TOKEN_2_VALID_TO = Instant.now()
            .plus(1, ChronoUnit.DAYS);

    public final static TimeSecret TOKEN_1 = TimeSecret.builder()
            .id(TOKEN_1_ID)
            .uid(USER_1_ID)
            .user(USER_1)
            .token(TOKEN_1_TOKEN)
            .processed(TOKEN_1_PROCESSED)
            .validTo(TOKEN_1_VALID_TO)
            .build();

    public final static TimeSecret TOKEN_2 = TimeSecret.builder()
            .id(TOKEN_2_ID)
            .uid(USER_2_ID)
            .user(USER_2)
            .token(TOKEN_2_TOKEN)
            .processed(TOKEN_2_PROCESSED)
            .validTo(TOKEN_2_VALID_TO)
            .build();

}
