package at.tuwien.mapper;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.user.UserDto;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Log4j2
@SpringBootTest
@MockAmqp
@MockOpensearch
public class UserMapperTest extends BaseUnitTest {

    @Test
    public void equals_fails() {

        /* test */
        assertNotEquals(USER_1_DTO, USER_2_DTO);
    }

    @Test
    public void equals_identity_succeeds() {

        /* test */
        assertEquals(USER_1_DTO, USER_1_DTO);
    }

    @Test
    public void equals_similar_succeeds() {
        final UserDto tmp = UserDto.builder()
                .id(USER_1_ID)
                .build();

        /* test */
        assertEquals(USER_1_DTO, tmp);
    }

}
