package at.tuwien;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchRestClientAutoConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Log4j2
@EnableJpaAuditing
@EnableTransactionManagement
@EntityScan(basePackages = {"at.tuwien.entities"})
@EnableElasticsearchRepositories(basePackages = {"at.tuwien.repository.sdb"})
@EnableJpaRepositories(basePackages = {"at.tuwien.repository.mdb"})
@SpringBootApplication(exclude = {ElasticsearchDataAutoConfiguration.class, ElasticsearchRestClientAutoConfiguration.class})
public class DbrepoDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DbrepoDataServiceApplication.class, args);
    }

}
