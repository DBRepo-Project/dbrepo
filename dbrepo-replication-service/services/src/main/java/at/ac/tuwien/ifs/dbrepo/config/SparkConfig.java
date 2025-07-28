package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
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

    @Value("${spark.hadoop.version}")
    private String hadoopVersion;

    @Value("${spark.awsSdk.version}")
    private String awsSdkVersion;

    @Value("${spark.mariadb.version}")
    private String mariadbVersion;

    @Bean
    public Map<String, String> sparkOptions() {
        final String[] packages = new String[]{
                "org.apache.hadoop:hadoop-aws:" + hadoopVersion,
                "org.mariadb.jdbc:mariadb-java-client:" + mariadbVersion,
                "software.amazon.awssdk:bundle:" + awsSdkVersion,
        };
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("spark.driver.extraJavaOptions", "-Divy.cache.dir=/tmp -Divy.home=/tmp");
        options.put("spark.ui.enabled", "false");
        options.put("spark.jars.packages", String.join(",", packages));
        return options;
    }

    @Bean
    public Map<String, String> hadoopOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("fs.s3a.access.key", s3AccessKeyId);
        options.put("fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider");
        options.put("fs.s3a.connection.establish.timeout", "30000");
        options.put("fs.s3a.connection.timeout", "200000");
        options.put("fs.s3a.connection.ssl.enabled", "false");
        options.put("fs.s3a.endpoint", s3Endpoint);
        options.put("fs.s3a.multipart.purge.age", "600000");
        options.put("fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem");
        options.put("fs.s3a.threads.keepalivetime", "60000");
        options.put("fs.s3a.path.style.access", "true");
        options.put("fs.s3a.secret.key", s3SecretAccessKey);
        return options;
    }

    @Bean
    public SparkConf sparkConf() {
        final SparkConf config = new SparkConf()
                .setMaster(computeEndpoint)
                .setAppName("data-service");
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
