package at.tuwien.mapper;

import at.tuwien.BaseUnitTest;
import at.tuwien.config.IndexConfig;
import at.tuwien.entities.user.User;
import at.tuwien.repository.sdb.UserIdxRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Log4j2
@SpringBootTest
public class UserMapperTest extends BaseUnitTest {

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private UserIdxRepository userIdxRepository;

    @Test
    public void equals_fails() {

        /* test */
        assertNotEquals(USER_1, USER_2);
    }

    @Test
    public void equals_identity_succeeds() {

        /* test */
        assertEquals(USER_1, USER_1);
    }

    @Test
    public void equals_similar_succeeds() {
        final User tmp = User.builder()
                .id(USER_1_ID)
                .build();

        /* test */
        assertEquals(USER_1, tmp);
    }

}
