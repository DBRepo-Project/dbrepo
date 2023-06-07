package at.tuwien;

import org.apache.jena.sys.JenaSystem;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;


@EnableJpaAuditing
@SpringBootApplication
@EntityScan(basePackages = {"at.tuwien.entities"})
@EnableElasticsearchRepositories(basePackages = {"at.tuwien.repository.sdb"})
@EnableJpaRepositories(basePackages = {"at.tuwien.repository.mdb"})
public class DbrepoSemanticsServiceApplication {

    public static void main(String[] args) {
        JenaSystem.init();
        SpringApplication.run(DbrepoSemanticsServiceApplication.class, args);
    }

}
