package at.tuwien.config;

import lombok.extern.log4j.Log4j2;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.opensearch.data.client.orhlc.ClientConfiguration;
import org.opensearch.data.client.orhlc.RestClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
public class OpenSearchConfig extends AbstractOpenSearchConfiguration {

    @Value("${spring.opensearch.uris}")
    private String openSearchEndpoint;

    @Value("${spring.opensearch.username}")
    private String openSearchUsername;

    @Value("${spring.opensearch.password}")
    private String openSearchPassword;

    @Bean
    @Override
    public RestHighLevelClient opensearchClient() {
        log.debug("open search endpoint: {}", openSearchEndpoint);
        final ClientConfiguration clientConfiguration = ClientConfiguration.builder()
                .connectedTo(openSearchEndpoint)
                .withBasicAuth(openSearchUsername, openSearchPassword)
                .build();
        return RestClients.create(clientConfiguration)
                .rest();
    }
}