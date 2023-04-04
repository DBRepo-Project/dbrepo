package at.tuwien;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@EnableWebSecurity
@SpringBootApplication
@EnableJpaAuditing
public class DbrepoAuthenticationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbrepoAuthenticationServiceApplication.class, args);
    }

}
