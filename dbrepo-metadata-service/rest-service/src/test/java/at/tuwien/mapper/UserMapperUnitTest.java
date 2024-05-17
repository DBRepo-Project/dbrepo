package at.tuwien.mapper;

import at.tuwien.test.AbstractUnitTest;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.api.user.UserDto;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

@Log4j2
@SpringBootTest
public class UserMapperUnitTest extends AbstractUnitTest {

    @Autowired
    private UserMapper userMapper;

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

    @Test
    public void userToUserBriefDto_succeeds() {

        /* test */
        final UserBriefDto response = userMapper.userToUserBriefDto(USER_1);
        assertEquals(USER_1_NAME, response.getName());
        assertEquals(USER_1_NAME + " — @" + USER_1_USERNAME, response.getQualifiedName());
    }

    @Test
    public void userToUserDto_succeeds() {

        /* test */
        final UserDto response = userMapper.userToUserDto(USER_1);
        assertEquals(USER_1_NAME, response.getName());
        assertEquals(USER_1_NAME + " — @" + USER_1_USERNAME, response.getQualifiedName());
    }

}
