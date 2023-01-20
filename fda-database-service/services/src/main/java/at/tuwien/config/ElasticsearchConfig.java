package at.tuwien.config;

import lombok.extern.log4j.Log4j2;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

@Log4j2
@Configuration
public class ElasticsearchConfig {

    @Value("${fda.elastic.endpoint}")
    private String elasticEndpoint;

    @Value("${fda.elastic.username}")
    private String elasticUsername;

    @Value("${fda.elastic.password}")
    private String elasticPassword;

    @Bean
    public RestHighLevelClient client() {
        log.debug("elastic endpoint={}", elasticEndpoint);
        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(elasticEndpoint)
                .build();
        return RestClients.create(clientConfiguration)
                .rest();
    }

    @Bean
    public ElasticsearchOperations elasticsearchTemplate() {
        return new ElasticsearchRestTemplate(client());
    }
}