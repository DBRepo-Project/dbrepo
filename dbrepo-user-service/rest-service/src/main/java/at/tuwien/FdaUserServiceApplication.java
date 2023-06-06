package at.tuwien;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableJpaAuditing
@EnableTransactionManagement
@EnableScheduling
@EntityScan(basePackages = {"at.tuwien.entities"})
@EnableElasticsearchRepositories(basePackages = {"at.tuwien.repository.sdb"})
@EnableJpaRepositories(basePackages = {"at.tuwien.repository.mdb"})
@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class})
public class FdaUserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FdaUserServiceApplication.class, args);
    }

}