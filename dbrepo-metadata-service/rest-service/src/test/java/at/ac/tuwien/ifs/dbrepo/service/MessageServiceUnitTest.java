package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageCreateDto;
import at.ac.tuwien.ifs.dbrepo.core.api.maintenance.BannerMessageUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.maintenance.BannerMessage;
import at.ac.tuwien.ifs.dbrepo.core.exception.MessageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.repository.BannerMessageRepository;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Log4j2
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class MessageServiceUnitTest extends BaseTest {

    @MockBean
    private BannerMessageRepository bannerMessageRepository;

    @Autowired
    private BannerMessageService bannerMessageService;

    @Test
    public void findAll_succeeds() {

        /* mock */
        when(bannerMessageRepository.findAll())
                .thenReturn(List.of(BANNER_MESSAGE_1, BANNER_MESSAGE_2));

        /* test */
        final List<BannerMessage> response = bannerMessageService.findAll();
        assertEquals(2, response.size());
    }

    @Test
    public void getActive_succeeds() {

        /* mock */
        when(bannerMessageRepository.findByActive())
                .thenReturn(List.of(BANNER_MESSAGE_1));

        /* test */
        final List<BannerMessage> response = bannerMessageService.getActive();
        assertEquals(1, response.size());
        final BannerMessage message0 = response.get(0);
        assertEquals(BANNER_MESSAGE_1_ID, message0.getId());
        assertEquals(BANNER_MESSAGE_1_MESSAGE, message0.getMessage());
        assertEquals(BANNER_MESSAGE_1_TYPE, message0.getType());
    }

    @Test
    public void find_succeeds() throws MessageNotFoundException {

        /* mock */
        when(bannerMessageRepository.findById(BANNER_MESSAGE_1_ID))
                .thenReturn(Optional.of(BANNER_MESSAGE_1));

        /* test */
        final BannerMessage response = bannerMessageService.find(BANNER_MESSAGE_1_ID);
        assertEquals(BANNER_MESSAGE_1_ID, response.getId());
        assertEquals(BANNER_MESSAGE_1_MESSAGE, response.getMessage());
        assertEquals(BANNER_MESSAGE_1_TYPE, response.getType());
    }

    @Test
    public void find_notFound_fails() {

        /* mock */
        when(bannerMessageRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        /* test */
        assertThrows(MessageNotFoundException.class, () -> {
            bannerMessageService.find(UUID.randomUUID());
        });
    }

    @Test
    public void create_succeeds() {
        final BannerMessageCreateDto request = BannerMessageCreateDto.builder()
                .message(BANNER_MESSAGE_1_MESSAGE)
                .type(BANNER_MESSAGE_1_TYPE_DTO)
                .build();

        /* mock */
        when(bannerMessageRepository.save(any(BannerMessage.class)))
                .thenReturn(BANNER_MESSAGE_1);

        /* test */
        final BannerMessage response = bannerMessageService.create(request);
        assertEquals(BANNER_MESSAGE_1_MESSAGE, response.getMessage());
        assertEquals(BANNER_MESSAGE_1_TYPE, response.getType());
    }

    @Test
    public void update_succeeds() {
        final BannerMessageUpdateDto request = BannerMessageUpdateDto.builder()
                .message(BANNER_MESSAGE_1_MESSAGE)
                .type(BANNER_MESSAGE_1_TYPE_DTO)
                .build();

        /* mock */
        when(bannerMessageRepository.save(any(BannerMessage.class)))
                .thenReturn(BANNER_MESSAGE_1);

        /* test */
        final BannerMessage response = bannerMessageService.update(BANNER_MESSAGE_1, request);
        assertEquals(BANNER_MESSAGE_1_MESSAGE, response.getMessage());
        assertEquals(BANNER_MESSAGE_1_TYPE, response.getType());
    }

    @Test
    public void delete_succeeds() {

        /* test */
        bannerMessageService.delete(BANNER_MESSAGE_1);
    }
}
