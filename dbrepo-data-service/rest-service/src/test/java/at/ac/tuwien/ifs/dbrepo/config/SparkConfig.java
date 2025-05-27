package at.ac.tuwien.ifs.dbrepo.config;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

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
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("spark.driver.extraJavaOptions", "-Divy.cache.dir=/tmp -Divy.home=/tmp");
        options.put("spark.ui.enabled", "false");
        return options;
    }

    @Bean
    public Map<String, String> hadoopOptions() {
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("fs.s3a.path.style.access", "true");
        options.put("fs.s3a.endpoint", s3Endpoint);
        options.put("fs.s3a.access.key", s3AccessKeyId);
        options.put("fs.s3a.secret.key", s3SecretAccessKey);
        return options;
    }

    @Bean
    public SparkConf sparkConf() {
        return new SparkConf()
                .setMaster("local[2]")
                .setAppName("junit");
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

    public Dataset<Row> loadDataset(String filename, String delimiter, Boolean withHeader, String... columns) {
        return sparkSession()
                .read()
                .format("csv")
                .option("header", withHeader)
                .option("delimiter", delimiter)
                .load(filename)
                .toDF(columns);
    }

}
