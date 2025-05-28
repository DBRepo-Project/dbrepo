package at.ac.tuwien.ifs.dbrepo.endpoints;

import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessage;
import at.ac.tuwien.ifs.dbrepo.core.exception.MessageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.service.BannerMessageService;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MessageEndpointUnitTest extends BaseTest {

    @MockBean
    private BannerMessageService bannerMessageService;

    @Autowired
    private MessageEndpoint messageEndpoint;

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
    @WithAnonymousUser
    public void list_onlyActive_succeeds() {

        /* mock */
        when(bannerMessageService.getActive())
                .thenReturn(List.of(BANNER_MESSAGE_1));

        /* test */
        final ResponseEntity<List<BannerMessageDto>> response = messageEndpoint.list(true);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    @WithAnonymousUser
    public void find_anonymous_succeeds() throws MessageNotFoundException {

        /* test */
        find_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username = USER_4_USERNAME)
    public void find_noRole_succeeds() throws MessageNotFoundException {

        /* test */
        find_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-maintenance-message"})
    public void find_hasRole_succeeds() throws MessageNotFoundException {

        /* test */
        find_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username = USER_1_USERNAME, authorities = {"find-maintenance-message"})
    public void find_hasRoleNotFound_fails() {

        /* test */
        assertThrows(MessageNotFoundException.class, () -> {
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
    @WithMockUser(username =USER_2_USERNAME, authorities = {"create-maintenance-message"})
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
    @WithMockUser(username =USER_2_USERNAME, authorities = {"update-maintenance-message"})
    public void update_hasRole_succeeds() throws MessageNotFoundException {

        /* test */
        update_generic(BANNER_MESSAGE_1_UPDATE_DTO, BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
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
    @WithMockUser(username =USER_2_USERNAME, authorities = {"delete-maintenance-message"})
    public void delete_hasRole_succeeds() throws MessageNotFoundException {

        /* test */
        delete_generic(BANNER_MESSAGE_1_ID, BANNER_MESSAGE_1);
    }

    @Test
    @WithMockUser(username =USER_2_USERNAME, authorities = {"delete-maintenance-message"})
    public void delete_hasRoleNotFound_fails() {

        /* test */
        assertThrows(MessageNotFoundException.class, () -> {
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
        final ResponseEntity<List<BannerMessageDto>> response = messageEndpoint.list(null);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void find_generic(UUID messageId, BannerMessage message) throws MessageNotFoundException {

        /* mock */
        if (message != null) {
            when(bannerMessageService.find(messageId))
                    .thenReturn(message);
        } else {
            doThrow(MessageNotFoundException.class)
                    .when(bannerMessageService)
                    .find(messageId);
        }

        /* test */
        final ResponseEntity<BannerMessageDto> response = messageEndpoint.find(messageId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void create_generic(BannerMessageCreateDto data, BannerMessage message) {

        /* mock */
        when(bannerMessageService.create(data))
                .thenReturn(message);

        /* test */
        final ResponseEntity<BannerMessageDto> response = messageEndpoint.create(data);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void update_generic(BannerMessageUpdateDto data, UUID messageId, BannerMessage message)
            throws MessageNotFoundException {

        /* mock */
        when(bannerMessageService.find(messageId))
                .thenReturn(message);
        when(bannerMessageService.update(message, data))
                .thenReturn(message);

        /* test */
        final ResponseEntity<BannerMessageDto> response = messageEndpoint.update(messageId, data);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    protected void delete_generic(UUID messageId, BannerMessage message) throws MessageNotFoundException {

        /* mock */
        if (message != null) {
            when(bannerMessageService.find(messageId))
                    .thenReturn(message);
        } else {
            doThrow(MessageNotFoundException.class)
                    .when(bannerMessageService)
                    .find(messageId);
        }
        doNothing()
                .when(bannerMessageService)
                .delete(message);

        /* test */
        final ResponseEntity<?> response = messageEndpoint.delete(messageId);
        assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
        assertNull(response.getBody());
    }
}
