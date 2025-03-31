package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class EndpointConfig {

    @Value("${dbrepo.website}")
    private String websiteUrl;

}
