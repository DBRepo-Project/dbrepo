package at.tuwien.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;


@Getter
@Configuration
public class InvenioConfig {

    @Value("${dev.token}")
    private String debugToken;

}
