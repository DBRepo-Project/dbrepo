package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteDoiEvent;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.BibliographyTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.CreateIdentifierDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierSaveDto;
import at.ac.tuwien.ifs.dbrepo.core.api.identifier.IdentifierTypeDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.IdentifierStatusType;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.gateway.DataCiteGateway;
import at.ac.tuwien.ifs.dbrepo.metadata.IdentifierRepository;
import at.ac.tuwien.ifs.dbrepo.service.IdentifierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Primary
@Profile("doi")
@Service
public class DataCiteIdentifierServiceImpl implements IdentifierService {

    private final DataCiteGateway dataCiteGateway;
    private final IdentifierService identifierService;
    private final IdentifierRepository identifierRepository;

    public DataCiteIdentifierServiceImpl(DataCiteGateway dataCiteGateway, IdentifierServiceImpl identifierService,
                                         IdentifierRepository identifierRepository) {
        this.dataCiteGateway = dataCiteGateway;
        this.identifierService = identifierService;
        this.identifierRepository = identifierRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findAll(IdentifierTypeDto type, UUID databaseId, UUID queryId, UUID viewId, UUID tableId) {
        return identifierService.findAll(type, databaseId, queryId, viewId, tableId);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Identifier publish(Identifier identifier) throws MalformedException, ExternalServiceException {
        identifier.setDoi(dataCiteGateway.save(identifier, DataCiteDoiEvent.PUBLISH));
        identifier.setStatus(IdentifierStatusType.PUBLISHED);
        return identifierRepository.save(identifier);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Identifier> findByDatabaseIdAndQueryId(UUID databaseId, UUID queryId) {
        return identifierService.findByDatabaseIdAndQueryId(databaseId, queryId);
    }

    @Override
    public List<Identifier> findAllDatabaseIdentifiers() {
        return identifierService.findAllDatabaseIdentifiers();
    }

    @Override
    public List<Identifier> findAllSubsetIdentifiers() {
        return identifierService.findAllSubsetIdentifiers();
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Identifier save(Database database, String ownedBy, IdentifierSaveDto data) throws DataServiceException,
            DataServiceConnectionException, MalformedException, DatabaseNotFoundException, IdentifierNotFoundException,
            ViewNotFoundException, QueryNotFoundException, SearchServiceException, SearchServiceConnectionException,
            ExternalServiceException {
        final Identifier entity = identifierService.save(database, ownedBy, data);
        dataCiteGateway.save(entity, null);
        return entity;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public Identifier create(Database database, String ownedBy, CreateIdentifierDto data) throws DataServiceException,
            DataServiceConnectionException, MalformedException, ViewNotFoundException, DatabaseNotFoundException,
            QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException,
            IdentifierNotFoundException {
        final Identifier entity = identifierService.create(database, ownedBy, data);
        entity.setDoi(dataCiteGateway.create());
        final Identifier identifier = identifierRepository.save(entity);
        dataCiteGateway.save(identifier, null);
        return identifier;
    }

    @Override
    public List<Identifier> findAll() {
        return identifierService.findAll();
    }

    @Override
    public List<Identifier> findAll(UUID databaseId) {
        return identifierService.findAll(databaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier find(UUID identifierId) throws IdentifierNotFoundException {
        return identifierService.find(identifierId);
    }

    @Override
    @Transactional(readOnly = true)
    public Identifier findByDoi(String doi) throws IdentifierNotFoundException {
        return identifierService.findByDoi(doi);
    }

    @Override
    @Transactional(readOnly = true)
    public InputStreamResource exportMetadata(Identifier identifier) {
        return identifierService.exportMetadata(identifier);
    }

    @Override
    @Transactional(readOnly = true)
    public String exportBibliography(Identifier identifier, BibliographyTypeDto style) throws MalformedException {
        return identifierService.exportBibliography(identifier, style);
    }

    @Override
    @Transactional
    public void delete(Identifier identifier) throws DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException {
        identifierService.delete(identifier);
    }

}
