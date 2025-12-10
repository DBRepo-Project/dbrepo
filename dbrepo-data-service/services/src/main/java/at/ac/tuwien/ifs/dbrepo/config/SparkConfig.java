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

    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${dbrepo.spark.hadoop.fs.s3a.endpoint}")
    private String s3aEndpoint;

    @Value("${dbrepo.spark.hadoop.fs.s3a.access.key}")
    private String s3aAccessKey;

    @Value("${dbrepo.spark.hadoop.fs.s3a.aws.credentials.provider}")
    private String s3aAwsCredentialsProvider;

    @Value("${dbrepo.spark.hadoop.fs.s3a.connection.timeout}")
    private String s3aConnectionTimeout;

    @Value("${dbrepo.spark.hadoop.fs.s3a.connection.establish.timeout}")
    private String s3aConnectionEstablishTimeout;

    @Value("${dbrepo.spark.hadoop.fs.s3a.secret.key}")
    private String s3aSecretKey;

    @Value("${dbrepo.spark.hadoop.fs.s3a.path.style.access}")
    private String s3aPathStyleAccess;

    @Value("${dbrepo.spark.hadoop.fs.s3a.connection.ssl.enabled}")
    private String s3aConnectionSslEnabled;

    @Value("${dbrepo.spark.hadoop.fs.s3a.multipart.purge.age}")
    private String s3aMultipartPurgeAge;

    @Value("${dbrepo.spark.hadoop.fs.s3a.impl}")
    private String s3aImpl;

    @Value("${dbrepo.spark.hadoop.fs.s3a.threads.keepalivetime}")
    private String s3aThreadsKeepalivetime;

    @Value("${dbrepo.spark.hadoop.fs.s3a.committer.name}")
    private String s3aCommitterName;

    @Value("${dbrepo.spark.hadoop.fs.s3a.change.detection.mode}")
    private String s3aChangeDetectionMode;

    @Value("${dbrepo.spark.hadoop.fs.s3a.multiobjectdelete.enable}")
    private String s3aMultiobjectdeleteEnable;

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
        // spark.hadoop.xxx
        final Map<String, String> options = new LinkedHashMap<>();
        options.put("fs.s3a.access.key", s3aAccessKey);
        options.put("fs.s3a.aws.credentials.provider", s3aAwsCredentialsProvider);
        options.put("fs.s3a.connection.timeout", s3aConnectionTimeout);
        options.put("fs.s3a.connection.establish.timeout", s3aConnectionEstablishTimeout);
        options.put("fs.s3a.connection.ssl.enabled", s3aConnectionSslEnabled);
        options.put("fs.s3a.endpoint", s3aEndpoint);
        options.put("fs.s3a.multipart.purge.age", s3aMultipartPurgeAge);
        options.put("fs.s3a.impl", s3aImpl);
        options.put("fs.s3a.threads.keepalivetime", s3aThreadsKeepalivetime);
        options.put("fs.s3a.path.style.access", s3aPathStyleAccess);
        options.put("fs.s3a.secret.key", s3aSecretKey);
        options.put("fs.s3a.committer.name", s3aCommitterName);
        // third-party compatibility flags
        options.put("fs.s3a.change.detection.mode", s3aChangeDetectionMode);
        options.put("fs.s3a.multiobjectdelete.enable", s3aMultiobjectdeleteEnable);
        return options;
    }

    @Bean
    public SparkConf sparkConf() {
        final SparkConf config = new SparkConf()
                .setMaster(computeEndpoint)
                .setAppName(applicationName);
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
