package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.List;

@Getter
@Log4j2
@Profile("doi")
@Configuration
public class DataCiteConfig {

    @Value("${dbrepo.datacite.url}")
    private String url;

    @Value("${dbrepo.datacite.prefix}")
    private String prefix;

    @Value("${dbrepo.datacite.username}")
    private String username;

    @Value("${dbrepo.datacite.password}")
    private String password;

    @Bean("dataCiteRestTemplate")
    public RestTemplate searchServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(url));
        log.debug("add basic authentication for data cite: username={}, password=(hidden)", username);
        restTemplate.getInterceptors()
                .add(new BasicAuthenticationInterceptor(username, password));
        return restTemplate;
    }

}
