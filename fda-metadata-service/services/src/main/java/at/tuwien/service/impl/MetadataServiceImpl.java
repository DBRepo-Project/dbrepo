package at.tuwien.service.impl;

import at.tuwien.config.MetadataConfig;
import at.tuwien.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class MetadataServiceImpl implements MetadataService {

    private final MetadataConfig metadataConfig;
    private final TemplateEngine templateEngine;

    @Autowired
    public MetadataServiceImpl(MetadataConfig metadataConfig, TemplateEngine templateEngine) {
        this.metadataConfig = metadataConfig;
        this.templateEngine = templateEngine;
    }

    @Override
    public String identify(String request) {
        final Context context = new Context();
        context.setVariable("responseDate", DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
                .withZone(ZoneId.systemDefault())
                .format(Instant.now()));
        context.setVariable("request", request);
        context.setVariable("repositoryName", metadataConfig.getRepositoryName());
        context.setVariable("baseURL", metadataConfig.getBaseUrl());
        context.setVariable("adminEmail", metadataConfig.getAdminEmail());
        context.setVariable("earliestDatestamp", metadataConfig.getEarliestDatestamp());
        context.setVariable("deletedRecord", metadataConfig.getDeletedRecord());
        context.setVariable("granularity", metadataConfig.getGranularity());
        return templateEngine.process("identify.xml", context);
    }

}
