package at.tuwien.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Getter
@Configuration
public class SecurityConfig {

    @Value("${fda.system.username}")
    private String systemUsername;

    @Value("${fda.system.passwd}")
    private String systemPassword;

}
