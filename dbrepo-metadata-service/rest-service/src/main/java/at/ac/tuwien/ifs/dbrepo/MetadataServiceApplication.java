package at.ac.tuwien.ifs.dbrepo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableJpaAuditing
@EnableAsync
@EnableTransactionManagement
@EntityScan(basePackages = {"at.ac.tuwien.ifs.dbrepo.core.entity"})
@EnableJpaRepositories(basePackages = {"at.ac.tuwien.ifs.dbrepo.metadata"})
@EnableRedisRepositories(basePackages = {"at.ac.tuwien.ifs.dbrepo.cache"})
@SpringBootApplication
public class MetadataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetadataServiceApplication.class, args);
    }

}
