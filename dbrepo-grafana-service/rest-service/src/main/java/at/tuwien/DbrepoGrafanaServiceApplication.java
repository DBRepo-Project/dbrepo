package at.tuwien;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DbrepoGrafanaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DbrepoGrafanaServiceApplication.class, args);
    }
}
