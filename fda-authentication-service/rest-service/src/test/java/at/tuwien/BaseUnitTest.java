package at.tuwien;

import at.tuwien.api.user.UserDetailsDto;
import at.tuwien.entities.user.RoleType;
import at.tuwien.entities.user.TimeSecret;
import at.tuwien.entities.user.Token;
import at.tuwien.entities.user.User;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.TestPropertySource;

import java.security.Principal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
            .roles(List.of(RoleType.ROLE_RESEARCHER))
            .lastModified(USER_1_LAST_MODIFIED)
            .build();

    public final static UserDetails USER_1_DETAILS = UserDetailsDto.builder()
            .username(USER_1_USERNAME)
            .email(USER_1_EMAIL)
            .password(USER_1_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
            .build();

    public final static Principal USER_1_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_1_DETAILS,
            USER_1_PASSWORD, USER_1_DETAILS.getAuthorities());


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
            .roles(List.of(RoleType.ROLE_RESEARCHER))
            .lastModified(USER_2_LAST_MODIFIED)
            .build();

    public final static UserDetails USER_2_DETAILS = UserDetailsDto.builder()
            .username(USER_2_USERNAME)
            .email(USER_2_EMAIL)
            .password(USER_2_PASSWORD)
            .authorities(List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER")))
            .build();

    public final static Principal USER_2_PRINCIPAL = new UsernamePasswordAuthenticationToken(USER_2_DETAILS,
            USER_2_PASSWORD, USER_2_DETAILS.getAuthorities());

    public final static Long TIME_SECRET_1_ID = 1L;
    public final static Boolean TIME_SECRET_1_PROCESSED = false;
    public final static String TIME_SECRET_1_TOKEN = "mysecrettokenrandomlygenerated";
    public final static Instant TIME_SECRET_1_VALID_TO = Instant.now()
            .plus(1, ChronoUnit.DAYS);

    public final static Long TIME_SECRET_2_ID = 2L;
    public final static Boolean TIME_SECRET_2_PROCESSED = true;
    public final static String TIME_SECRET_2_TOKEN = "blahblahblah";
    public final static Instant TIME_SECRET_2_VALID_TO = Instant.now()
            .plus(1, ChronoUnit.DAYS);

    public final static TimeSecret TIME_SECRET_1 = TimeSecret.builder()
            .id(TIME_SECRET_1_ID)
            .uid(USER_1_ID)
            .user(USER_1)
            .token(TIME_SECRET_1_TOKEN)
            .processed(TIME_SECRET_1_PROCESSED)
            .validTo(TIME_SECRET_1_VALID_TO)
            .build();

    public final static TimeSecret TIME_SECRET_2 = TimeSecret.builder()
            .id(TIME_SECRET_2_ID)
            .uid(USER_2_ID)
            .user(USER_2)
            .token(TIME_SECRET_2_TOKEN)
            .processed(TIME_SECRET_2_PROCESSED)
            .validTo(TIME_SECRET_2_VALID_TO)
            .build();

    public final static Long TOKEN_1_ID = 1L;
    public final static Instant TOKEN_1_EXPIRES = Instant.now().plus(100000000, ChronoUnit.MILLIS);

    public final static Token TOKEN_1 = Token.builder()
            .id(TOKEN_1_ID)
            .expires(TOKEN_1_EXPIRES)
            .build();

    public final static Long TOKEN_2_ID = 2L;
    public final static Instant TOKEN_2_EXPIRES = Instant.now().plus(100000000, ChronoUnit.MILLIS);

    public final static Token TOKEN_2 = Token.builder()
            .id(TOKEN_2_ID)
            .expires(TOKEN_2_EXPIRES)
            .build();

}
