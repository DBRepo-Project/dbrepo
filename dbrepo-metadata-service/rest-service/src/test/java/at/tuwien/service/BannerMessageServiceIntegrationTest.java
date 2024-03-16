package at.tuwien.service;

import at.tuwien.BaseUnitTest;
import at.tuwien.annotations.MockAmqp;
import at.tuwien.annotations.MockListeners;
import at.tuwien.annotations.MockOpensearch;
import at.tuwien.api.maintenance.BannerMessageCreateDto;
import at.tuwien.api.maintenance.BannerMessageTypeDto;
import at.tuwien.api.maintenance.BannerMessageUpdateDto;
import at.tuwien.entities.maintenance.BannerMessage;
import at.tuwien.entities.maintenance.BannerMessageType;
import at.tuwien.exception.BannerMessageNotFoundException;
import at.tuwien.repository.mdb.BannerMessageRepository;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Log4j2
@EnableAutoConfiguration(exclude = RabbitAutoConfiguration.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
@SpringBootTest
@ExtendWith(SpringExtension.class)
@MockAmqp
@MockListeners
@MockOpensearch
public class BannerMessageServiceIntegrationTest extends BaseUnitTest {

    @Autowired
    private BannerMessageRepository bannerMessageRepository;

    @Autowired
    private BannerMessageService bannerMessageService;

    @BeforeEach
    public void beforeEach() {
        genesis();
        /* metadata database */
        bannerMessageRepository.save(BANNER_MESSAGE_1);
        bannerMessageRepository.save(BANNER_MESSAGE_2);
    }

    @Test
    public void findAll_succeeds() {

        /* test */
        final List<BannerMessage> response = bannerMessageService.findAll();
        assertEquals(2, response.size());
    }

    @Test
    public void getActive_succeeds() {

        /* test */
        final List<BannerMessage> response = bannerMessageService.getActive();
        assertEquals(1, response.size());
        final BannerMessage message0 = response.get(0);
        assertEquals(BANNER_MESSAGE_1_ID, message0.getId());
        assertEquals(BANNER_MESSAGE_1_MESSAGE, message0.getMessage());
        assertEquals(BANNER_MESSAGE_1_TYPE, message0.getType());
    }

    @Test
    public void find_succeeds() throws BannerMessageNotFoundException {

        /* test */
        final BannerMessage response = bannerMessageService.find(BANNER_MESSAGE_1_ID);
        assertEquals(BANNER_MESSAGE_1_ID, response.getId());
        assertEquals(BANNER_MESSAGE_1_MESSAGE, response.getMessage());
        assertEquals(BANNER_MESSAGE_1_TYPE, response.getType());
    }

    @Test
    public void find_notFound_fails() {

        /* test */
        assertThrows(BannerMessageNotFoundException.class, () -> {
            bannerMessageService.find(9999L);
        });
    }

    @Test
    public void create_succeeds() {
        final BannerMessageCreateDto request = BannerMessageCreateDto.builder()
                .message("test")
                .type(BannerMessageTypeDto.INFO)
                .build();

        /* test */
        final BannerMessage response = bannerMessageService.create(request);
        assertEquals("test", response.getMessage());
        assertEquals(BannerMessageType.INFO, response.getType());
    }

    @Test
    public void update_succeeds() throws BannerMessageNotFoundException {
        final BannerMessageUpdateDto request = BannerMessageUpdateDto.builder()
                .message("test")
                .type(BannerMessageTypeDto.INFO)
                .build();

        /* test */
        final BannerMessage response = bannerMessageService.update(BANNER_MESSAGE_1_ID, request);
        assertEquals("test", response.getMessage());
        assertEquals(BannerMessageType.INFO, response.getType());
    }

    @Test
    public void update_notFound_fails() {
        final BannerMessageUpdateDto request = BannerMessageUpdateDto.builder()
                .message("test")
                .type(BannerMessageTypeDto.INFO)
                .build();

        /* test */
        assertThrows(BannerMessageNotFoundException.class, () -> {
            bannerMessageService.update(9999L, request);
        });
    }

    @Test
    public void delete_succeeds() throws BannerMessageNotFoundException {

        /* test */
        bannerMessageService.delete(BANNER_MESSAGE_1_ID);
    }

    @Test
    public void delete_notFound_fails() {

        /* test */
        assertThrows(BannerMessageNotFoundException.class, () -> {
            bannerMessageService.delete(9999L);
        });
    }
}
