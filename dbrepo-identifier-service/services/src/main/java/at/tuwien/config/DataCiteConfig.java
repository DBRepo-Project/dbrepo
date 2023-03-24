package at.tuwien.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Getter
@Profile("doi")
@Configuration
public class DataCiteConfig {

    @Value("${fda.datacite.url}")
    private String url;

    @Value("${fda.datacite.prefix}")
    private String prefix;

    @Value("${fda.datacite.username}")
    private String username;

    @Value("${fda.datacite.password}")
    private String password;
}
