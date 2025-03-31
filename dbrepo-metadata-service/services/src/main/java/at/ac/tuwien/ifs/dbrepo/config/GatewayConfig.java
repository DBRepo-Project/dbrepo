package at.ac.tuwien.ifs.dbrepo.config;

import at.ac.tuwien.ifs.dbrepo.auth.InternalRequestInterceptor;
import at.ac.tuwien.ifs.dbrepo.service.CredentialService;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Log4j2
@Getter
@Configuration
public class GatewayConfig {

    @Value("${dbrepo.endpoints.brokerService}")
    private String brokerEndpoint;

    @Value("${dbrepo.endpoints.dataService}")
    private String dataEndpoint;

    @Value("${dbrepo.endpoints.analyseService}")
    private String analyseEndpoint;

    @Value("${dbrepo.endpoints.searchService}")
    private String searchEndpoint;

    @Value("${dbrepo.endpoints.dashboardService}")
    private String dashboardEndpoint;

    @Value("${dbrepo.endpoints.rorService}")
    private String rorEndpoint;

    @Value("${dbrepo.endpoints.crossRefService}")
    private String crossRefEndpoint;

    @Value("${dbrepo.endpoints.doiService}")
    private String doiEndpoint;

    @Value("${spring.rabbitmq.username}")
    private String brokerUsername;

    @Value("${spring.rabbitmq.password}")
    private String brokerPassword;

    @Value("${dbrepo.system.username}")
    private String systemUsername;

    @Value("${dbrepo.system.password}")
    private String systemPassword;
    
    private final CredentialService credentialService;

    @Autowired
    public GatewayConfig(CredentialService credentialService) {
        this.credentialService = credentialService;
    }

    @Profile("!junit")
    @Bean("brokerRestTemplate")
    public RestTemplate brokerRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(brokerEndpoint));
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(credentialService, this));
        return restTemplate;
    }

    @Bean("dataServiceRestTemplate")
    public RestTemplate dataServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(dataEndpoint));
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(credentialService, this));
        return restTemplate;
    }

    @Bean("analyseServiceRestTemplate")
    public RestTemplate analyseServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(analyseEndpoint));
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(credentialService, this));
        return restTemplate;
    }

    @Bean("searchServiceRestTemplate")
    public RestTemplate searchServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(searchEndpoint));
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(credentialService, this));
        return restTemplate;
    }

    @Bean("dashboardServiceRestTemplate")
    public RestTemplate dashboardServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(dashboardEndpoint));
        restTemplate.getInterceptors()
                .add(new InternalRequestInterceptor(credentialService, this));
        return restTemplate;
    }

    @Bean("doiServiceRestTemplate")
    public RestTemplate doiServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(doiEndpoint));
        return restTemplate;
    }

    @Bean("crossRefServiceRestTemplate")
    public RestTemplate crossRefServiceRestTemplate() {
        final RestTemplate restTemplate = new RestTemplate();
        restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(crossRefEndpoint));
        return restTemplate;
    }

}
