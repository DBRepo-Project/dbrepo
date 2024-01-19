package at.tuwien.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class EndpointConfig {

    @Value("${fda.website}")
    private String websiteUrl;

}
