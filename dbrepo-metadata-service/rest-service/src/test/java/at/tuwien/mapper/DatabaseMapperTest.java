package at.tuwien.mapper;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.user.UserDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.QueryMalformedException;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class DatabaseMapperTest extends BaseUnitTest {

    @Autowired
    private DatabaseMapper databaseMapper;

    @Test
    public void databaseToDatabaseDto_succeeds() {
        final Database debug = DATABASE_1;

        /* test */
        final DatabaseDto response = databaseMapper.databaseToDatabaseDto(DATABASE_1);
        assertEquals(DATABASE_1_ID, response.getId());
        assertEquals(DATABASE_1_NAME, response.getName());
        assertEquals(DATABASE_1_EXCHANGE, response.getExchangeName());
        assertEquals(DATABASE_1_DESCRIPTION, response.getDescription());
        assertEquals(DATABASE_1_INTERNALNAME, response.getInternalName());
        assertEquals(DATABASE_1_CREATED, response.getCreated());
        final UserDto creator = response.getCreator();
        assertEquals(USER_1_ID, creator.getId());
        assertEquals(USER_1_USERNAME, creator.getUsername());
        final UserDto owner = response.getOwner();
        assertEquals(USER_1_ID, owner.getId());
        assertEquals(USER_1_USERNAME, owner.getUsername());
    }

    @Test
    public void userToRawCreateUserQuery_fails () {
        final User request = User.builder()
                .username("username")
                .mariadbPassword(null) // <<<<<<<<<
                .build();

        /* test */
        assertThrows(QueryMalformedException.class, () -> {
            databaseMapper.userToRawCreateUserQuery(null, request);
        });
    }

}
