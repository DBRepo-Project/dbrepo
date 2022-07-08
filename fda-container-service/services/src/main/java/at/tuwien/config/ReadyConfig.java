package at.tuwien.config;

import at.tuwien.seeder.Seeder;
import com.google.common.io.Files;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.TimeZone;

@Log4j2
@Configuration
public class ReadyConfig {

    @Value("${fda.ready.path}")
    private String readyPath;

    private final Seeder seederImpl;

    @Autowired
    public ReadyConfig(Seeder seederImpl) {
        this.seederImpl = seederImpl;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() throws IOException {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        seederImpl.seed();
        Files.touch(new File(readyPath));
    }

}
