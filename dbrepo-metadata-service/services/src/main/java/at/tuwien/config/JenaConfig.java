package at.tuwien.config;

import lombok.Getter;
import org.apache.jena.query.Dataset;
import org.apache.jena.query.DatasetFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class JenaConfig {

    @Value("${dbrepo.connectionTimeout}")
    private Integer connectionTimeout;

    @Bean
    public Dataset dataset() {
        return DatasetFactory.create();
    }
}
