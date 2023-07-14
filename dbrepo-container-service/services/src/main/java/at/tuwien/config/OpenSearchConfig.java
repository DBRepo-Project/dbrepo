package at.tuwien.config;

import lombok.extern.log4j.Log4j2;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestClientBuilder;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.sniff.NodesSniffer;
import org.opensearch.client.sniff.OpenSearchNodesSniffer;
import org.opensearch.client.sniff.Sniffer;
import org.opensearch.data.client.orhlc.AbstractOpenSearchConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Log4j2
@Configuration
public class OpenSearchConfig extends AbstractOpenSearchConfiguration {

    @Value("${spring.opensearch.host}")
    private String openSearchHost;

    @Value("${spring.opensearch.port}")
    private Integer openSearchPort;

    @Value("${spring.opensearch.protocol}")
    private String openSearchProtocol;

    @Value("${spring.opensearch.username}")
    private String openSearchUsername;

    @Value("${spring.opensearch.password}")
    private String openSearchPassword;

    @Bean
    @Override
    public RestHighLevelClient opensearchClient() {
        log.debug("open search endpoint: {}://{}:{}", openSearchProtocol, openSearchHost, openSearchPort);
        final CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(openSearchUsername, openSearchPassword));
        RestClientBuilder builder = RestClient.builder(new HttpHost(openSearchHost, openSearchPort, openSearchProtocol))
                .setHttpClientConfigCallback(httpClientBuilder -> httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider));
        return new RestHighLevelClient(builder);
    }

    @Bean
    public Sniffer nodesSniffer() {
        final NodesSniffer nodesSniffer = new OpenSearchNodesSniffer(opensearchClient().getLowLevelClient(),
                TimeUnit.SECONDS.toMillis(5), OpenSearchNodesSniffer.Scheme.HTTP);
        return Sniffer.builder(opensearchClient().getLowLevelClient())
                .setNodesSniffer(nodesSniffer)
                .build();

    }
}