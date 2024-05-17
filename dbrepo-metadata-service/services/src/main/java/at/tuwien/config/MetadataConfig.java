package at.tuwien.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class MetadataConfig {

    @Value("${dbrepo.repository-name}")
    private String repositoryName;

    @Value("${dbrepo.base-url}")
    private String baseUrl;

    @Value("${dbrepo.admin-email}")
    private String adminEmail;

    @Value("${dbrepo.deleted-record}")
    private String deletedRecord;

    @Value("${dbrepo.granularity}")
    private String granularity;

    @Value("${dbrepo.pid.base}")
    private String pidBase;

}
