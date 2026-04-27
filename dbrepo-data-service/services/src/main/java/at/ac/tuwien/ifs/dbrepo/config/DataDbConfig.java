package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
public class DataDbConfig {

    @Value("${dbrepo.defaultSchema}")
    private String defaultSchema;

}
