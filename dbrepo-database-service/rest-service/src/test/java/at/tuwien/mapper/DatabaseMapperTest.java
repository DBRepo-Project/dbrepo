package at.tuwien.mapper;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseDto;
import at.tuwien.api.user.UserBriefDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseMapperTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @Autowired
    private DatabaseMapper databaseMapper;

    @Test
    public void databaseToDatabaseDto_succeeds() {

        /* test */
        final DatabaseDto response = databaseMapper.databaseToDatabaseDto(DATABASE_1);
        assertEquals(DATABASE_1_ID, response.getId());
        assertEquals(DATABASE_1_NAME, response.getName());
        assertEquals(DATABASE_1_EXCHANGE, response.getExchangeName());
        assertEquals(DATABASE_1_DESCRIPTION, response.getDescription());
        assertEquals(DATABASE_1_INTERNALNAME, response.getInternalName());
        assertEquals(DATABASE_1_CREATED, response.getCreated());
        final UserBriefDto creator = response.getCreator();
        assertEquals(USER_1_ID, creator.getId());
        assertEquals(USER_1_USERNAME, creator.getUsername());
        assertEquals(USER_1_THEME, creator.getThemeDark());
        final UserBriefDto owner = response.getOwner();
        assertEquals(USER_1_ID, owner.getId());
        assertEquals(USER_1_USERNAME, owner.getUsername());
        assertEquals(USER_1_THEME, owner.getThemeDark());
    }

}
