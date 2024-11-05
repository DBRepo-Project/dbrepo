package at.tuwien.config;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Log4j2
@Getter
@Configuration
public class SparkConfig {

    @Value("${dbrepo.endpoints.computeService}")
    private String computeEndpoint;

    @Value("${dbrepo.endpoints.storageService}")
    private String s3Endpoint;

    @Value("${dbrepo.s3.accessKeyId}")
    private String s3AccessKeyId;

    @Value("${dbrepo.s3.secretAccessKey}")
    private String s3SecretAccessKey;

    @Bean
    public Map<String, String> sparkOptions() {
        return new LinkedHashMap<>() {{
            put("spark.driver.extraJavaOptions", "-Divy.cache.dir=/tmp -Divy.home=/tmp");
            put("spark.ui.enabled", "false");
        }};
    }

    @Bean
    public Map<String, String> hadoopOptions() {
        return new LinkedHashMap<>() {{
            put("fs.s3a.path.style.access", "true");
            put("fs.s3a.endpoint", s3Endpoint);
            put("fs.s3a.access.key", s3AccessKeyId);
            put("fs.s3a.secret.key", s3SecretAccessKey);
        }};
    }

    @Bean
    public SparkConf sparkConf() {
        final SparkConf config = new SparkConf()
                .setMaster(computeEndpoint)
                .setAppName("dbrepo-data-service");
        sparkOptions()
                .forEach(config::set);
        return config;
    }

    @Bean
    public SparkSession sparkSession() {
        final SparkSession spark = SparkSession.builder()
                .config(sparkConf())
                .getOrCreate();
        hadoopOptions()
                .forEach((key, value) -> spark.sparkContext()
                        .hadoopConfiguration()
                        .set(key, value));
        return spark;
    }

}
