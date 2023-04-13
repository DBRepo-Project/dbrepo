package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.config.IndexConfig;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.repository.jpa.*;
import at.tuwien.test.BaseTest;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class AccessServiceUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexConfig indexConfig;

    @MockBean
    private Channel channel;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private DatabaseAccessRepository databaseAccessRepository;

    @Autowired
    private AccessService accessService;

    @Test
    public void list_succeeds() throws AccessDeniedException {

        /* mock */
        when(databaseAccessRepository.findByHdbid(DATABASE_1_ID))
                .thenReturn(List.of(DATABASE_1_RESEARCHER_READ_ACCESS, DATABASE_2_RESEARCHER_READ_ACCESS));

        /* test */
        final List<DatabaseAccess> response = accessService.list(DATABASE_1_ID);
        assertEquals(2, response.size());
    }

    @Test
    public void list_empty_succeeds() throws AccessDeniedException {

        /* mock */
        when(databaseAccessRepository.findByHdbid(DATABASE_1_ID))
                .thenReturn(List.of());

        /* test */
        final List<DatabaseAccess> response = accessService.list(DATABASE_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void find_succeeds() throws AccessDeniedException {

        /* mock */
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(Optional.of(DATABASE_1_RESEARCHER_READ_ACCESS));

        /* test */
        final DatabaseAccess response = accessService.find(DATABASE_1_ID, USER_1_USERNAME);
        assertEquals(AccessType.READ, response.getType());
    }

    @Test
    public void find_fails() {

        /* mock */
        when(databaseAccessRepository.findByDatabaseIdAndUsername(DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            accessService.find(DATABASE_1_ID, USER_1_USERNAME);
        });
    }

    @Test
    public void update_isOwner_fails() {
        final DatabaseModifyAccessDto request = DatabaseModifyAccessDto.builder()
                .type(AccessTypeDto.READ)
                .build();

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));
        when(userRepository.findByUsername(USER_1_USERNAME))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessService.update(CONTAINER_1_ID, DATABASE_1_ID, USER_1_USERNAME, request);
        });
    }

}
