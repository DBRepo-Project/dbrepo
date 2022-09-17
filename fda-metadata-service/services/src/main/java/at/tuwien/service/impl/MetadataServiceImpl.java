package at.tuwien.service.impl;

import at.tuwien.OaiErrorType;
import at.tuwien.OaiListIdentifiersParameters;
import at.tuwien.config.MetadataConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.MetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.List;

@Service
public class MetadataServiceImpl implements MetadataService {

    private final MetadataConfig metadataConfig;
    private final TemplateEngine templateEngine;
    private final MetadataMapper metadataMapper;
    private final IdentifierService identifierService;

    @Autowired
    public MetadataServiceImpl(MetadataConfig metadataConfig, TemplateEngine templateEngine,
                               MetadataMapper metadataMapper, IdentifierService identifierService) {
        this.metadataConfig = metadataConfig;
        this.templateEngine = templateEngine;
        this.metadataMapper = metadataMapper;
        this.identifierService = identifierService;
    }

    @Override
    public String identify() {
        final Context context = new Context();
        context.setVariable("repositoryName", metadataConfig.getRepositoryName());
        context.setVariable("baseURL", metadataConfig.getBaseUrl());
        context.setVariable("adminEmail", metadataConfig.getAdminEmail());
        context.setVariable("earliestDatestamp", metadataConfig.getEarliestDatestamp());
        context.setVariable("deletedRecord", metadataConfig.getDeletedRecord());
        context.setVariable("granularity", metadataConfig.getGranularity());
        final String body = templateEngine.process("identify.xml", context);
        return parseResponse("verb=\"Identify\"", body);
    }

    @Override
    public String listIdentifiers(OaiListIdentifiersParameters parameters) {
        final StringBuilder builder = new StringBuilder("<ListIdentifiers>");
        final List<Identifier> identifiers = identifierService.findAll();
        identifiers.forEach(identifier -> {
            final Context context = new Context();
            context.setVariable("identifier", metadataConfig.getPidBase() + identifier.getId());
            context.setVariable("datestamp", metadataMapper.instantToDatestamp(identifier.getCreated()));
            builder.append(templateEngine.process("record.xml", context));
        });
        builder.append("</ListIdentifiers>");
        return parseResponse(parameters.getParametersString(), builder.toString());
    }

    @Override
    public String error(OaiErrorType type) {
        final Context context = new Context();
        context.setVariable("code", type.getErrorCode());
        context.setVariable("message", type.getErrorText());
        final String body = templateEngine.process("error.xml", context);
        return parseResponse(body);
    }

    private String requestUrl() {
        final ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
        builder.scheme("https");
        return builder.build().toUriString();
    }

    private String parseResponse(String body) {
        return parseResponse("", body);
    }

    private String parseResponse(String parameterString, String body) {
        final Context context = new Context();
        context.setVariable("responseDate", metadataMapper.instantToDatestamp(Instant.now()));
        context.setVariable("request", "<request" + (parameterString != null ? parameterString : "") + ">" + requestUrl() + "</request>");
        context.setVariable("body", body);
        return templateEngine.process("_header.xml", context);
    }

}
