package at.tuwien.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Value("${app.version:unknown}")
    private String version;

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Database Repository Authentication Service API")
                        .contact(new Contact()
                                .name("Prof. Andreas Rauber")
                                .email("andreas.rauber@tuwien.ac.at"))
                        .description("Service that manages the authentication")
                        .version(version)
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Sourcecode Documentation")
                        .url("https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("container-service")
                .pathsToMatch("/api/**")
                .build();
    }

}
