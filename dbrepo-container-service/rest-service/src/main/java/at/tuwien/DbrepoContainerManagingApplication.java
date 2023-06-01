package at.tuwien;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"at.tuwien.repository.jpa"})
@EntityScan(basePackages = {"at.tuwien.entities"})
public class DbrepoContainerManagingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbrepoContainerManagingApplication.class, args);
    }

}
