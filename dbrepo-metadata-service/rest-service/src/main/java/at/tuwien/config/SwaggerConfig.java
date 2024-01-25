package at.tuwien.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Database Repository Metadata Service API")
                        .contact(new Contact()
                                .name("Prof. Andreas Rauber")
                                .email("andreas.rauber@tuwien.ac.at"))
                        .description("Service that manages the metadata")
                        .version("__APPVERSION__")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .externalDocs(new ExternalDocumentation()
                        .description("Sourcecode Documentation")
                        .url("https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services"))
                .servers(List.of(new Server()
                                .description("Generated server url")
                                .url("http://localhost:9099"),
                        new Server()
                                .description("Sandbox")
                                .url("https://test.dbrepo.tuwien.ac.at")));
    }

    @Bean
    public GroupedOpenApi publicApi() {
        return GroupedOpenApi.builder()
                .group("metadata-service")
                .pathsToMatch("/api/**")
                .build();
    }

}
