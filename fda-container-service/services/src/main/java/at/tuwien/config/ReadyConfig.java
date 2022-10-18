package at.tuwien.config;

import at.tuwien.exception.ImageNotFoundException;
import at.tuwien.service.ImageService;
import com.google.common.io.Files;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.io.File;
import java.io.IOException;

@Log4j2
@Configuration
public class ReadyConfig {

    private final ImageService imageService;
    private final static String imageRepository = "mariadb";
    private final static String imageTag = "10.5";

    @Autowired
    public ReadyConfig(ImageService imageService) {
        this.imageService = imageService;
    }

    @Value("${fda.ready.path}")
    private String readyPath;

    @EventListener(ApplicationReadyEvent.class)
    public void init() throws IOException, ImageNotFoundException {
        if (!imageService.exists(imageRepository, imageTag)) {
            log.debug("image {}:{} is not present on the host", imageRepository, imageTag);
            log.debug("pulling image {}:{}", imageRepository, imageTag);
            imageService.pull(imageRepository, imageTag);
        } else {
            log.debug("image {}:{} is present on the host", imageRepository, imageTag);
            log.debug("skip pulling image {}:{}", imageRepository, imageTag);
        }
        Files.touch(new File(readyPath));
        log.info("Service is ready");
    }

}
