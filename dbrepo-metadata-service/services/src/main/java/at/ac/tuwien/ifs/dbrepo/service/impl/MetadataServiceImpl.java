package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.config.MetadataConfig;
import at.ac.tuwien.ifs.dbrepo.core.api.crossref.CrossRefDto;
import at.ac.tuwien.ifs.dbrepo.core.api.orcid.OrcidDto;
import at.ac.tuwien.ifs.dbrepo.core.api.ror.RorDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.external.ExternalMetadataDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierStatusType;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.CrossRefGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.OrcidGateway;
import at.ac.tuwien.ifs.dbrepo.gateway.RorGateway;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiErrorType;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiListIdentifiersParameters;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiListRecordsParameters;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiListParameters;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiRecordParameters;
import at.ac.tuwien.ifs.dbrepo.metadata.IdentifierRepository;
import at.ac.tuwien.ifs.dbrepo.service.IdentifierService;
import at.ac.tuwien.ifs.dbrepo.service.MetadataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.unbescape.xml.XmlEscape;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
public class MetadataServiceImpl implements MetadataService {

    private final RorGateway rorGateway;
    private final OrcidGateway orcidGateway;
    private final MetadataConfig metadataConfig;
    private final MetadataMapper metadataMapper;
    private final TemplateEngine templateEngine;
    private final CrossRefGateway crossRefGateway;
    private final IdentifierService identifierService;
    private final IdentifierRepository identifierRepository;

    @Autowired
    public MetadataServiceImpl(RorGateway rorGateway, OrcidGateway orcidGateway, MetadataConfig metadataConfig,
                               MetadataMapper metadataMapper, TemplateEngine templateEngine,
                               CrossRefGateway crossRefGateway, IdentifierService identifierService,
                               IdentifierRepository identifierRepository) {
        this.rorGateway = rorGateway;
        this.orcidGateway = orcidGateway;
        this.metadataConfig = metadataConfig;
        this.metadataMapper = metadataMapper;
        this.templateEngine = templateEngine;
        this.crossRefGateway = crossRefGateway;
        this.identifierService = identifierService;
        this.identifierRepository = identifierRepository;
    }

    @Override
    public String identify() {
        final Optional<Identifier> optional = identifierRepository.findEarliest();
        final String earliest = optional.map(o -> harvestDate(o.getCreated())).orElse(null);
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
        if (recordTemplate(parameters.getMetadataPrefix()) == null) return error(OaiErrorType.CANNOT_DISSEMINATE_FORMAT);
        final StringBuilder builder = new StringBuilder("<ListIdentifiers>");
        final Harvest harvest = selectForHarvest(parameters);
        if (harvest.error() != null) return harvest.error();
        final List<Identifier> identifiers = harvest.records();
        log.debug("found {} identifiers", identifiers.size());
        final List<Identifier> available = availableInFormat(identifiers, parameters.getMetadataPrefix());
        if (available.isEmpty()) return parseResponse(parameters.getParametersString(), errorBody(OaiErrorType.NO_RECORDS_MATCH));
        available.forEach(identifier -> {
            final Context context = new Context();
            context.setVariable("identifier", identifier);
            context.setVariable("pid", identifier.getDoi() != null ? ("doi:" + identifier.getDoi()) : ("oai:" + identifier.getId()));
            context.setVariable("datestamp", harvestDate(identifier));
            context.setVariable("setspec", "openaire_data");
            builder.append(templateEngine.process("identifier.xml", context));
        });
        builder.append("</ListIdentifiers>");
        return parseResponse(parameters.getParametersString(), builder.toString());
    }

    @Transactional(readOnly = true)
    @Override
    public String listRecords(OaiListRecordsParameters parameters) {
        final String template = recordTemplate(parameters.getMetadataPrefix());
        if (template == null) {
            return error(OaiErrorType.CANNOT_DISSEMINATE_FORMAT);
        }
        final StringBuilder builder = new StringBuilder("<ListRecords>");
        final Harvest harvest = selectForHarvest(parameters);
        if (harvest.error() != null) return harvest.error();
        final List<Identifier> identifiers = harvest.records();
        log.debug("found {} identifiers", identifiers.size());
        final List<Identifier> available = availableInFormat(identifiers, parameters.getMetadataPrefix());
        if (available.isEmpty()) return parseResponse(parameters.getParametersString(), errorBody(OaiErrorType.NO_RECORDS_MATCH));
        available.forEach(identifier -> {
            final Context context = new Context();
            context.setVariable("identifier", identifier);
            context.setVariable("pid", identifier.getDoi() != null ? ("doi:" + identifier.getDoi()) : ("oai:" + identifier.getId()));
            context.setVariable("datestamp", harvestDate(identifier));
            context.setVariable("setspec", "openaire_data");
            context.setVariable("identifierType", identifier.getDoi() != null ? "DOI" : "OAI");
            builder.append(templateEngine.process(template, Set.of("record"), context));
        });
        builder.append("</ListRecords>");
        return parseResponse(parameters.getParametersString(), builder.toString());
    }

    @Override
    @Transactional(readOnly = true)
    public String getRecord(OaiRecordParameters parameters) throws IdentifierNotFoundException {
        final Identifier identifier = publishedIdentifier(parameters.getIdentifier());
        final String templateFileName = recordTemplate(parameters.getMetadataPrefix());
        if (templateFileName == null || !supportsFormat(identifier, parameters.getMetadataPrefix())) {
            return parseResponse(parameters.getParametersString(), errorBody(OaiErrorType.CANNOT_DISSEMINATE_FORMAT));
        }
        final Context context = new Context();
        context.setVariable("identifier", identifier);
        context.setVariable("identifierType", identifier.getDoi() != null ? "DOI" : "OAI");
        context.setVariable("setspec", "openaire_data");
        context.setVariable("pid", identifier.getDoi() != null ? ("doi:" + identifier.getDoi()) : ("oai:" + identifier.getId()));
        context.setVariable("datestamp", harvestDate(identifier));
        final String body = parseResponse(parameters.getParametersString(), templateEngine.process(templateFileName, context));
        log.trace("mapped body {}", body);
        return body;
    }

    private Identifier publishedIdentifier(String pid) throws IdentifierNotFoundException {
        final Identifier identifier;
        if (pid.startsWith("doi:")) {
            identifier = identifierService.findByDoi(pid.substring(4));
        } else if (pid.startsWith("oai:")) {
            try {
                identifier = identifierService.find(UUID.fromString(pid.substring(4)));
            } catch (IllegalArgumentException e) {
                throw new IdentifierNotFoundException("Invalid OAI identifier");
            }
        } else {
            throw new IdentifierNotFoundException("Unsupported identifier prefix");
        }
        if (!IdentifierStatusType.PUBLISHED.equals(identifier.getStatus())) {
            throw new IdentifierNotFoundException("Identifier is not available through OAI-PMH");
        }
        return identifier;
    }

    private List<Identifier> availableInFormat(List<Identifier> identifiers, String prefix) {
        return "oai_datacite".equals(prefix) ? identifiers.stream().filter(Identifier::hasDataCiteRegistration).toList() : identifiers;
    }

    private boolean supportsFormat(Identifier identifier, String prefix) {
        return !"oai_datacite".equals(prefix) || identifier.hasDataCiteRegistration();
    }

    private String recordTemplate(String prefix) {
        return switch (prefix == null ? "oai_dc" : prefix) {
            case "oai_datacite" -> "record_oai_datacite.xml";
            case "oai_dc" -> "record_oai_dc.xml";
            default -> null;
        };
    }

    @Override
    public String listMetadataFormats() {
        return metadataFormats("verb=\"ListMetadataFormats\"", true);
    }

    @Override
    @Transactional(readOnly = true)
    public String listMetadataFormats(String pid) throws IdentifierNotFoundException {
        final Identifier identifier = publishedIdentifier(pid);
        final String args = "verb=\"ListMetadataFormats\" identifier=\"" + XmlEscape.escapeXml10AttributeMinimal(pid) + "\"";
        return metadataFormats(args, identifier.hasDataCiteRegistration());
    }

    private String metadataFormats(String args, boolean dataCite) {
        final Context context = new Context();
        context.setVariable("dataCite", dataCite);
        return parseResponse(args, "<ListMetadataFormats>" + templateEngine.process("metadata-format.xml", context) + "</ListMetadataFormats>");
    }

    @Override
    public String listSets() {
        String builder = "<ListSets>" + templateEngine.process("sets.xml", new Context()) +
                "</ListSets>";
        return parseResponse("verb=\"ListSets\"", builder);
    }

    @Override
    public String error(OaiErrorType type) {
        return parseResponse(errorBody(type));
    }

    private String errorBody(OaiErrorType type) {
        final Context context = new Context();
        context.setVariable("code", type.getErrorCode());
        context.setVariable("message", type.getErrorText());
        return templateEngine.process("error.xml", context);
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

    @Override
    public ExternalMetadataDto findByUrl(String url) throws OrcidNotFoundException, RorNotFoundException,
            DoiNotFoundException, IdentifierNotSupportedException {
        if (url.contains("orcid.org")) {
            final OrcidDto orcidDto = orcidGateway.findByUrl(url);
            return metadataMapper.orcidDtoToExternalMetadataDto(orcidDto);
        } else if (url.contains("ror.org")) {
            final int idx = url.lastIndexOf('/');
            if (idx + 1 >= url.length()) {
                log.error("Failed to find metadata from ROR URL: too short");
                throw new RorNotFoundException("Failed to find metadata from ROR URL: too short");
            }
            final String id = url.substring(idx + 1);
            final RorDto rorDto = rorGateway.findById(id);
            return metadataMapper.rorDtoToExternalMetadataDto(rorDto);
        } else if (url.contains("doi.org")) {
            final int idx = url.indexOf("doi.org/");
            if (idx + 1 >= url.length()) {
                log.error("Failed to find metadata from CrossRef URL: too short");
                throw new RorNotFoundException("Failed to find metadata from CrossRef URL: too short");
            }
            final String id = url.substring(idx + 8);
            final CrossRefDto crossrefDto = crossRefGateway.findById(id);
            return metadataMapper.crossrefDtoToExternalMetadataDto(crossrefDto);
        }
        log.error("Failed to find metadata: unsupported identifier {}", url);
        throw new IdentifierNotSupportedException("Failed to find metadata: unsupported identifier " + url);
    }

    private record Harvest(List<Identifier> records, String error) {}

    private Harvest selectForHarvest(OaiListParameters parameters) {
        final OaiErrorType error = parameters.argumentError();
        if (error == OaiErrorType.BAD_ARGUMENT) return new Harvest(List.of(), error(error));
        if (error != null) return new Harvest(List.of(), parseResponse(parameters.getParametersString(), errorBody(error)));

        final OaiListParameters.DateRange range;
        try {
            range = parameters.dateRange("YYYY-MM-DD".equals(metadataConfig.getGranularity()));
        } catch (DateTimeException e) {
            return new Harvest(List.of(), error(OaiErrorType.BAD_ARGUMENT));
        }
        // All published identifiers belong to the single set advertised by ListSets.
        final List<Identifier> identifiers = parameters.getSet() == null || OaiListParameters.SET_SPEC.equals(parameters.getSet())
                ? identifierRepository.findForHarvest(range.from(), range.untilExclusive()) : List.of();
        return identifiers.isEmpty()
                ? new Harvest(List.of(), parseResponse(parameters.getParametersString(), errorBody(OaiErrorType.NO_RECORDS_MATCH)))
                : new Harvest(identifiers, null);
    }

    private String harvestDate(Identifier identifier) {
        final Instant modified = identifier.getLastModified();
        return harvestDate(modified != null && modified.isAfter(identifier.getCreated()) ? modified : identifier.getCreated());
    }

    private String harvestDate(Instant date) {
        return "YYYY-MM-DD".equals(metadataConfig.getGranularity())
                ? date.atZone(ZoneOffset.UTC).toLocalDate().toString()
                : metadataMapper.instantToDatestamp(date);
    }

}
