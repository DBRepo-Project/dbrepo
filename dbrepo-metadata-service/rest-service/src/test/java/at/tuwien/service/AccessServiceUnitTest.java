package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.DatabaseModifyAccessDto;
import at.tuwien.entities.database.AccessType;
import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.exception.AccessDeniedException;
import at.tuwien.exception.DatabaseNotFoundException;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.repository.mdb.DatabaseRepository;
import at.tuwien.repository.mdb.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class AccessServiceUnitTest extends BaseUnitTest {

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private UserRepository userRepository;

    @Autowired
    private AccessService accessService;

    @BeforeEach
    public void beforeEach() {
        DATABASE_1.setAccesses(List.of(DATABASE_1_USER_1_READ_ACCESS, DATABASE_1_USER_2_WRITE_OWN_ACCESS, DATABASE_1_USER_3_WRITE_ALL_ACCESS));
    }

    @Test
    public void list_succeeds() throws DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final List<DatabaseAccess> response = accessService.list(DATABASE_1_ID);
        assertEquals(3, response.size());
    }

    @Test
    public void list_empty_succeeds() throws DatabaseNotFoundException {

        /* mock */
        DATABASE_1.setAccesses(List.of());
        doReturn(Optional.of(DATABASE_1))
                .when(databaseRepository)
                .findById(DATABASE_1_ID);
        /* test */
        final List<DatabaseAccess> response = accessService.list(DATABASE_1_ID);
        assertEquals(0, response.size());
    }

    @Test
    public void find_succeeds() throws AccessDeniedException, DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        final DatabaseAccess response = accessService.find(DATABASE_1_ID, USER_1_ID);
        assertEquals(AccessType.READ, response.getType());
    }

    @Test
    public void find_fails() {

        /* mock */
        DATABASE_1.setAccesses(List.of());
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        /* test */
        assertThrows(AccessDeniedException.class, () -> {
            accessService.find(DATABASE_1_ID, USER_1_ID);
        });
    }

    @Test
    public void find_databaseNotFound_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            accessService.find(DATABASE_1_ID, USER_1_ID);
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
        when(userRepository.findById(USER_1_ID))
                .thenReturn(Optional.of(USER_1));

        /* test */
        assertThrows(NotAllowedException.class, () -> {
            accessService.update(DATABASE_1_ID, USER_1_ID, request);
        });
    }

}
