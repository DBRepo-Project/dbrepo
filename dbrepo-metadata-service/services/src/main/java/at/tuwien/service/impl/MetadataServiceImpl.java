package at.tuwien.service.impl;

import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.api.orcid.OrcidDto;
import at.tuwien.api.ror.RorDto;
import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.config.MetadataConfig;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import at.tuwien.gateway.CrossrefGateway;
import at.tuwien.gateway.OrcidGateway;
import at.tuwien.gateway.RorGateway;
import at.tuwien.mapper.ExternalMapper;
import at.tuwien.mapper.MetadataMapper;
import at.tuwien.oaipmh.OaiErrorType;
import at.tuwien.oaipmh.OaiListIdentifiersParameters;
import at.tuwien.oaipmh.OaiRecordParameters;
import at.tuwien.repository.IdentifierRepository;
import at.tuwien.service.IdentifierService;
import at.tuwien.service.MetadataService;
import at.tuwien.utils.XmlUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {

    private final RorGateway rorGateway;
    private final OrcidGateway orcidGateway;
    private final ExternalMapper externalMapper;
    private final MetadataConfig metadataConfig;
    private final MetadataMapper metadataMapper;
    private final TemplateEngine templateEngine;
    private final CrossrefGateway crossrefGateway;
    private final IdentifierService identifierService;
    private final IdentifierRepository identifierRepository;

    @Autowired
    public MetadataServiceImpl(RorGateway rorGateway, OrcidGateway orcidGateway, ExternalMapper externalMapper,
                               MetadataConfig metadataConfig, MetadataMapper metadataMapper,
                               TemplateEngine templateEngine, CrossrefGateway crossrefGateway,
                               IdentifierService identifierService, IdentifierRepository identifierRepository) {
        this.rorGateway = rorGateway;
        this.orcidGateway = orcidGateway;
        this.externalMapper = externalMapper;
        this.metadataConfig = metadataConfig;
        this.metadataMapper = metadataMapper;
        this.templateEngine = templateEngine;
        this.crossrefGateway = crossrefGateway;
        this.identifierService = identifierService;
        this.identifierRepository = identifierRepository;
    }

    @Override
    public String identify() {
        final Optional<Identifier> optional = identifierRepository.findEarliest();
        final String earliest = optional.map(o -> o.getCreated().toString()).orElse(null);
        final Context context = new Context();
        context.setVariable("repositoryName", metadataConfig.getRepositoryName());
        context.setVariable("baseURL", metadataConfig.getBaseUrl());
        context.setVariable("adminEmail", metadataConfig.getAdminEmail());
        context.setVariable("earliestDatestamp", earliest);
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
            context.setVariable("identifier", identifier);
            context.setVariable("pid", identifier.getDoi() != null ? ("doi:" + identifier.getDoi()) : ("oai:" + identifier.getId()));
            context.setVariable("datestamp", metadataMapper.instantToDatestamp(identifier.getCreated()));
            builder.append(templateEngine.process("identifier.xml", context));
        });
        builder.append("</ListIdentifiers>");
        return parseResponse(parameters.getParametersString(), builder.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public String getRecord(OaiRecordParameters parameters) throws IdentifierNotFoundException {
        /* find identifier */
        final Identifier identifier;
        if (parameters.getIdentifier().startsWith("doi")) {
            identifier = identifierService.findByDoi(parameters.getIdentifier().substring(4));
        } else if (parameters.getIdentifier().startsWith("oai")) {
            identifier = identifierService.find(Long.parseLong(parameters.getIdentifier().substring(4)));
        } else {
            final String prefix = parameters.getIdentifier().substring(0, 3);
            log.error("Invalid prefix: {}", prefix);
            throw new IdentifierNotFoundException("Invalid prefix: " + prefix);
        }
        final String templateFileName = "record_" + (parameters.getMetadataPrefix() == null ? "oai_datacite" : parameters.getMetadataPrefix()) + ".xml";
        final Context context = new Context();
        context.setVariable("identifier", identifier);
        context.setVariable("identifierType", identifier.getDoi() != null ? "DOI" : "OAI");
        context.setVariable("pid", identifier.getDoi() != null ? ("doi:" + identifier.getDoi()) : ("oai:" + identifier.getId()));
        context.setVariable("datestamp", metadataMapper.instantToDatestamp(identifier.getCreated()));
        final String body = parseResponse(parameters.getParametersString(), templateEngine.process(templateFileName, context));
        log.trace("mapped body {}", body);
        return body;
    }

    @Override
    public String listMetadataFormats() {
        final StringBuilder builder = new StringBuilder("<ListMetadataFormats>");
        builder.append(templateEngine.process("metadata-format.xml", new Context()));
        builder.append("</ListMetadataFormats>");
        return XmlUtil.pretty(parseResponse("verb=\"ListMetadataFormats\"", builder.toString()));
    }

    @Override
    public String error(OaiErrorType type) {
        final Context context = new Context();
        context.setVariable("code", type.getErrorCode());
        context.setVariable("message", type.getErrorText());
        final String body = templateEngine.process("error.xml", context);
        log.trace("mapped error {}", type);
        return XmlUtil.pretty(parseResponse(body));
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
        return XmlUtil.pretty(templateEngine.process("_header.xml", context));
    }

    @Override
    public ExternalMetadataDto findByUrl(String url) throws OrcidNotFoundException, RorNotFoundException,
            DoiNotFoundException, IdentifierNotSupportedException {
        if (url.contains("orcid.org")) {
            final OrcidDto orcidDto = orcidGateway.findByUrl(url);
            return externalMapper.orcidDtoToExternalMetadataDto(orcidDto);
        } else if (url.contains("ror.org")) {
            final int idx = url.lastIndexOf('/');
            if (idx + 1 >= url.length()) {
                log.error("Failed to find metadata from ROR URL: too short");
                throw new RorNotFoundException("Failed to find metadata from ROR URL: too short");
            }
            final String id = url.substring(idx + 1);
            final RorDto rorDto = rorGateway.findById(id);
            return externalMapper.rorDtoToExternalMetadataDto(rorDto);
        } else if (url.contains("doi.org")) {
            final int idx = url.indexOf("doi.org/");
            if (idx + 1 >= url.length()) {
                log.error("Failed to find metadata from CrossRef URL: too short");
                throw new RorNotFoundException("Failed to find metadata from CrossRef URL: too short");
            }
            final String id = url.substring(idx + 8);
            final CrossrefDto crossrefDto = crossrefGateway.findById(id);
            return externalMapper.crossrefDtoToExternalMetadataDto(crossrefDto);
        }
        log.error("Failed to find metadata: unsupported identifier {}", url);
        throw new IdentifierNotSupportedException("Failed to find metadata: unsupported identifier " + url);
    }

}
