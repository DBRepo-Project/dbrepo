package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.api.database.DatabaseCreateDto;
import at.tuwien.config.IndexInitializer;
import at.tuwien.config.ReadyConfig;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.*;
import at.tuwien.repository.jpa.ContainerRepository;
import at.tuwien.repository.jpa.DatabaseRepository;
import at.tuwien.service.impl.MariaDbServiceImpl;
import at.tuwien.service.impl.RabbitMqServiceImpl;
import com.rabbitmq.client.Channel;
import lombok.extern.log4j.Log4j2;
import org.apache.http.auth.BasicUserPrincipal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class DatabaseServiceUnitTest extends BaseUnitTest {

    @MockBean
    private ReadyConfig readyConfig;

    @MockBean
    private IndexInitializer indexInitializer;

    @MockBean
    private Channel channel;

    @MockBean
    private RabbitMqServiceImpl rabbitMqService;

    @Autowired
    private MariaDbServiceImpl databaseService;

    @MockBean
    private DatabaseRepository databaseRepository;

    @MockBean
    private ContainerRepository containerRepository;

    @Test
    public void findAll_succeeds() {
        /* mock */
        when(databaseRepository.findAll(CONTAINER_1_ID))
                .thenReturn(List.of(DATABASE_1));

        /* test */
        final List<Database> response = databaseService.findAll(CONTAINER_1_ID);
        assertEquals(1, response.size());
        assertEquals(DATABASE_1, response.get(0));
    }

    @Test
    public void findById_succeeds() throws DatabaseNotFoundException {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.of(DATABASE_1));

        final Database response = databaseService.findById(CONTAINER_1_ID, DATABASE_1_ID);

        /* test */
        assertEquals(DATABASE_1, response);
    }

    @Test
    public void findById_notFound_fails() {

        /* mock */
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.findById(CONTAINER_1_ID, DATABASE_1_ID);
        });
    }

    @Test
    public void delete_notFound_fails() {

        /* mock */
        when(containerRepository.findById(CONTAINER_1_ID))
                .thenReturn(Optional.of(mock(Container.class)));
        when(databaseRepository.findById(DATABASE_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(DatabaseNotFoundException.class, () -> {
            databaseService.delete(CONTAINER_1_ID, DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void create_notFound_fails() {
        final DatabaseCreateDto request = DatabaseCreateDto.builder()
                .name(DATABASE_1_NAME)
                .build();

        /* mock */
        when(containerRepository.findById(CONTAINER_1_ID))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(ContainerNotFoundException.class, () -> {
            databaseService.create(CONTAINER_1_ID, request, USER_1_PRINCIPAL);
        });
    }

    @Test
    public void delete_image_fails() {
        final ContainerImage image = ContainerImage.builder()
                .repository("mysql")
                .build();
        final Container container = Container.builder()
                .image(image)
                .build();
        final Database database = Database.builder()
                .container(container)
                .build();

        /* mock */
        when(containerRepository.findById(CONTAINER_1_ID))
                .thenReturn(Optional.of(CONTAINER_1));
        when(databaseRepository.findPublicOrMine(CONTAINER_1_ID, DATABASE_1_ID, USER_1_USERNAME))
                .thenReturn(Optional.of(database));

        /* test */
        assertThrows(ImageNotSupportedException.class, () -> {
            databaseService.delete(CONTAINER_1_ID, DATABASE_1_ID, USER_1_PRINCIPAL);
        });
    }

}
