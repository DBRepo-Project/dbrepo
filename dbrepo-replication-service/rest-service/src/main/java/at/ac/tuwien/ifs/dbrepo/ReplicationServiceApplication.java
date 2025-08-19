package at.ac.tuwien.ifs.dbrepo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Slf4j
@EnableJpaRepositories(basePackages = {"at.ac.tuwien.ifs.dbrepo.repository"})
@EntityScan(basePackages = {"at.ac.tuwien.ifs.dbrepo.core.entity"})
@SpringBootApplication
public class ReplicationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReplicationServiceApplication.class, args);
    }

}
