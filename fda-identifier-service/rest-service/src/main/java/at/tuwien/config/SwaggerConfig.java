package at.tuwien.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Database Repository Table Service API")
                        .contact(new Contact()
                                .name("Prof. Andreas Rauber")
                                .email("andreas.rauber@tuwien.ac.at"))
                        .description("Service that manages the tables")
                        .version("v1.1.0-alpha")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Wiki Documentation")
                        .url("https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/wikis"));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("identifier-service")
                .pathsToMatch("/api/**")
                .build();
    }

}
