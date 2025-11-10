package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class MetadataConfig {

    @Value("${dbrepo.baseUrl}")
    private String baseUrl;

}
