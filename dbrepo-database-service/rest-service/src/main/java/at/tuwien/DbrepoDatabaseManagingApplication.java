package at.tuwien;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaAuditing
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = {"at.tuwien.repository.jpa"})
@EnableElasticsearchRepositories(basePackages = {"at.tuwien.repository.elastic"})
@EntityScan(basePackages = {"at.tuwien.entities"})
public class DbrepoDatabaseManagingApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbrepoDatabaseManagingApplication.class, args);
    }

}
