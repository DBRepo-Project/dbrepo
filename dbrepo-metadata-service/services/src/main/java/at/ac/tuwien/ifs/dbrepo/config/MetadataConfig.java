package at.ac.tuwien.ifs.dbrepo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class MetadataConfig {

    @Value("${dbrepo.repositoryName}")
    private String repositoryName;

    @Value("${dbrepo.baseUrl}")
    private String baseUrl;

    @Value("${dbrepo.adminEmail}")
    private String adminEmail;

    @Value("${dbrepo.deletedRecord}")
    private String deletedRecord;

    @Value("${dbrepo.granularity}")
    private String granularity;

    @Value("${dbrepo.pid.base}")
    private String pidBase;

}
