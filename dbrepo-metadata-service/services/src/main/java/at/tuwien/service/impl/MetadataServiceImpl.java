package at.tuwien.service.impl;

import at.tuwien.oaipmh.OaiErrorType;
import at.tuwien.oaipmh.OaiListIdentifiersParameters;
import at.tuwien.oaipmh.OaiRecordParameters;
import at.tuwien.config.MetadataConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.MetadataService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.Instant;
import java.util.List;

@Log4j2
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
    @Transactional(readOnly = true)
    public String listIdentifiers(OaiListIdentifiersParameters parameters) {
        final StringBuilder builder = new StringBuilder("<ListIdentifiers>");
        final List<Identifier> identifiers = identifierService.findAll();
        log.debug("found {} identifiers", identifiers.size());
        identifiers.forEach(identifier -> {
            final Context context = new Context();
            context.setVariable("identifier", metadataConfig.getPidBase() + identifier.getId());
            context.setVariable("datestamp", metadataMapper.instantToDatestamp(identifier.getCreated()));
            builder.append(templateEngine.process("identifier.xml", context));
        });
        builder.append("</ListIdentifiers>");
        return parseResponse(parameters.getParametersString(), builder.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public String getRecord(OaiRecordParameters parameters) throws IdentifierNotFoundException {
        final Long id = Long.parseLong(parameters.getIdentifier());
        final Identifier identifier = identifierService.find(id);
        final Context context = new Context();
        context.setVariable("identifier", identifier.getId());
        context.setVariable("creators", identifier.getCreators());
        context.setVariable("datestamp", metadataMapper.instantToDatestamp(identifier.getCreated()));
        context.setVariable("titles", identifier.getTitles());
        context.setVariable("descriptions", identifier.getDescriptions());
        context.setVariable("publisher", identifier.getPublisher());
        return parseResponse(parameters.getParametersString(), templateEngine.process("record.xml", context));
    }

    @Override
    public String listMetadataFormats() {
        final StringBuilder builder = new StringBuilder("<ListMetadataFormats>");
        builder.append(templateEngine.process("metadata-format.xml", new Context()));
        builder.append("</ListMetadataFormats>");
        return parseResponse("verb=\"ListMetadataFormats\"", builder.toString());
    }

    @Override
    public String error(OaiErrorType type) {
        final Context context = new Context();
        context.setVariable("code", type.getErrorCode());
        context.setVariable("message", type.getErrorText());
        final String body = templateEngine.process("error.xml", context);
        log.trace("mapped error {}", type);
        return parseResponse(body);
    }

    private String requestUrl() {
        final ServletUriComponentsBuilder builder = ServletUriComponentsBuilder.fromCurrentRequestUri();
        builder.scheme("https");
        return builder.build()
                .toUriString();
    }

    private String parseResponse(String body) {
        return parseResponse(null, body);
    }

    private String parseResponse(String parameterString, String body) {
        final Context context = new Context();
        context.setVariable("responseDate", metadataMapper.instantToDatestamp(Instant.now()));
        if (parameterString == null) {
            context.setVariable("request", "<request>" + requestUrl() + "</request>");
        } else {
            context.setVariable("request", "<request " + parameterString + ">" + requestUrl() + "</request>");
        }
        context.setVariable("body", body);
        return templateEngine.process("_header.xml", context);
    }

}
