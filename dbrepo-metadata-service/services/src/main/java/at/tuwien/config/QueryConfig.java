package at.tuwien.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class QueryConfig {

    @Value("${fda.privileges}")
    private String grantPrivileges;

    @Value("${fda.unsupported}")
    private String[] notSupportedKeywords;

    @Value("${fda.sharedFilesystem}")
    private String sharedFilesystem;

    @Value("${fda.deleteAfterImport}")
    private Boolean deleteAfterImport;

}
