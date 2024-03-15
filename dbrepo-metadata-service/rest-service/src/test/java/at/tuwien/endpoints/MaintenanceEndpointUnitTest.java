package at.tuwien.endpoints;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.maintenance.BannerMessageBriefDto;
import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.exception.BannerMessageNotFoundException;
import at.tuwien.service.BannerMessageService;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockOpensearch
public class MaintenanceEndpointUnitTest extends BaseUnitTest {

    @MockBean
    private BannerMessageService bannerMessageService;

    @Autowired
    private MaintenanceEndpoint maintenanceEndpoint;

    @Test
    @WithAnonymousUser
    public void list_anonymous_succeeds() {

        /* test */
        list_generic();
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void list_noRole_succeeds() {

        /* test */
        list_generic();
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"list-maintenance-messages"})
    public void list_hasRole_succeeds() {

        /* test */
        list_generic();
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_succeeds() throws BannerMessageNotFoundException {

        /* test */
        find_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void find_noRole_succeeds() throws BannerMessageNotFoundException {

        /* test */
        find_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-maintenance-message"})
    public void find_hasRole_succeeds() throws BannerMessageNotFoundException {

        /* test */
        find_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-maintenance-message"})
    public void find_hasRoleNotFound_fails() {

        /* test */
        assertThrows(BannerMessageNotFoundException.class, () -> {
            find_generic(BANNER_MESSAGE_1_ID, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void create_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(BANNER_MESSAGE_1_CREATE_DTO, BANNER_MESSAGE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void create_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            create_generic(BANNER_MESSAGE_1_CREATE_DTO, BANNER_MESSAGE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"create-maintenance-message"})
    public void create_hasRole_succeeds() {

        /* test */
        create_generic(BANNER_MESSAGE_1_CREATE_DTO, BANNER_MESSAGE_1);
    }

    @Test
    @WithAnonymousUser
    public void update_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            update_generic(BANNER_MESSAGE_1_UPDATE_DTO, BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void update_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            update_generic(BANNER_MESSAGE_1_UPDATE_DTO, BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"update-maintenance-message"})
    public void update_hasRole_succeeds() throws BannerMessageNotFoundException {

        /* test */
        update_generic(BANNER_MESSAGE_1_UPDATE_DTO, BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"update-maintenance-message"})
    public void update_hasRoleNotFound_fails() {

        /* test */
        assertThrows(BannerMessageNotFoundException.class, () -> {
            update_generic(BANNER_MESSAGE_1_UPDATE_DTO, BANNER_MESSAGE_1_ID, null);
        });
    }

    @Test
    @WithAnonymousUser
    public void delete_anonymous_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void delete_noRole_fails() {

        /* test */
        assertThrows(org.springframework.security.access.AccessDeniedException.class, () -> {
            delete_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
        });
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-maintenance-message"})
    public void delete_hasRole_succeeds() throws BannerMessageNotFoundException {

        /* test */
        delete_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username = USER_2_USERNAME, authorities = {"delete-maintenance-message"})
    public void delete_hasRoleNotFound_fails() {

        /* test */
        assertThrows(BannerMessageNotFoundException.class, () -> {
            delete_generic(BANNER_MESSAGE_1_ID, null);
        });
    }

    /* ################################################################################################### */
    /* ## GENERIC TEST CASES                                                                            ## */
    /* ################################################################################################### */

    protected void list_generic() {

        /* mock */
        when(bannerMessageService.findAll())
                .thenReturn(List.of(BANNER_MESSAGE_1, BANNER_MESSAGE_2));

        /* test */
        final ResponseEntity<List<BannerMessageDto>> response = maintenanceEndpoint.list("");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final List<BannerMessageDto> body = response.getBody();
        assertEquals(2, body.size());
        final BannerMessageDto message0 = body.get(0);
        assertEquals(BANNER_MESSAGE_1_ID, message0.getId());
        assertEquals(BANNER_MESSAGE_1_TYPE_DTO, message0.getType());
        assertEquals(BANNER_MESSAGE_1_MESSAGE, message0.getMessage());
        final BannerMessageDto message1 = body.get(1);
        assertEquals(BANNER_MESSAGE_2_ID, message1.getId());
        assertEquals(BANNER_MESSAGE_2_TYPE_DTO, message1.getType());
        assertEquals(BANNER_MESSAGE_2_MESSAGE, message1.getMessage());
    }

    protected void find_generic(Long messageId, BannerMessage message) throws BannerMessageNotFoundException {

        /* mock */
        if (message != null) {
            when(bannerMessageService.find(messageId))
                    .thenReturn(message);
        } else {
            doThrow(BannerMessageNotFoundException.class)
                    .when(bannerMessageService)
                    .find(messageId);
        }

        /* test */
        final ResponseEntity<BannerMessageDto> response = maintenanceEndpoint.find(messageId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        final BannerMessageDto body = response.getBody();
        assertEquals(BANNER_MESSAGE_1_ID, body.getId());
        assertEquals(BANNER_MESSAGE_1_MESSAGE, body.getMessage());
        assertEquals(BANNER_MESSAGE_1_TYPE_DTO, body.getType());
        assertEquals(BANNER_MESSAGE_1_START, body.getDisplayStart());
        assertEquals(BANNER_MESSAGE_1_END, body.getDisplayEnd());
    }

    protected void create_generic(BannerMessageCreateDto data, BannerMessage message) {

        /* mock */
        when(bannerMessageService.create(data))
                .thenReturn(message);

        /* test */
        final ResponseEntity<BannerMessageDto> response = maintenanceEndpoint.create(data);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void update_generic(BannerMessageUpdateDto data, Long messageId, BannerMessage message)
            throws BannerMessageNotFoundException {

        /* mock */
        if (message != null) {
            when(bannerMessageService.update(messageId, data))
                    .thenReturn(message);
        } else {
            doThrow(BannerMessageNotFoundException.class)
                    .when(bannerMessageService)
                    .update(messageId, data);
        }

        /* test */
        final ResponseEntity<BannerMessageDto> response = maintenanceEndpoint.update(messageId, data);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void delete_generic(Long messageId, BannerMessage message)
            throws BannerMessageNotFoundException {

        /* mock */
        if (message != null) {
            when(bannerMessageService.find(messageId))
                    .thenReturn(message);
            doNothing()
                    .when(bannerMessageService)
                    .delete(messageId);
        } else {
            doThrow(BannerMessageNotFoundException.class)
                    .when(bannerMessageService)
                    .delete(messageId);
        }

        /* test */
        final ResponseEntity<?> response = maintenanceEndpoint.delete(messageId);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }
}
