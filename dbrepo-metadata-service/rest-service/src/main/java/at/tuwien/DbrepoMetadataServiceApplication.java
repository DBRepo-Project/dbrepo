package at.tuwien;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableJpaAuditing
@EnableTransactionManagement
@EntityScan(basePackages = {"at.tuwien.entities"})
@EnableJpaRepositories(basePackages = {"at.tuwien.repository"})
@SpringBootApplication
public class DbrepoMetadataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbrepoMetadataServiceApplication.class, args);
    }

}
