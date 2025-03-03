package at.tuwien.service;

import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.api.identifier.CreateIdentifierDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import at.tuwien.exception.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface IdentifierService {

    /**
     * Finds all identifiers.
     *
     * @return List of identifiers.
     */
    List<Identifier> findAll();

    /**
     * Finds all identifiers in the metadata database for a database with given id.
     *
     * @param databaseId The database id.
     * @return The list of identifiers.
     */
    List<Identifier> findAll(UUID databaseId);

    /**
     * Finds an identifier by given id.
     *
     * @param id The identifier id.
     * @return The identifier, if successful.
     * @throws IdentifierNotFoundException The identifier does not exist.
     */
    Identifier find(UUID id) throws IdentifierNotFoundException;

    /**
     * Finds an identifier by given doi.
     *
     * @param doi The identifier doi.
     * @return The identifier, if successful.
     * @throws IdentifierNotFoundException The identifier does not exist.
     */
    Identifier findByDoi(String doi) throws IdentifierNotFoundException;

    /**
     * Finds all identifiers in the metadata database which are not deleted and filter by query id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return The list of identifiers.
     */
    List<Identifier> findByDatabaseIdAndQueryId(UUID databaseId, UUID queryId);

    /**
     * Finds all identifiers in the metadata database which are identifying databases.
     *
     * @return The list of identifiers.
     */
    List<Identifier> findAllDatabaseIdentifiers();

    /**
     * Finds all identifiers in the metadata database which are identifying subsets.
     *
     * @return The list of identifiers.
     */
    List<Identifier> findAllSubsetIdentifiers();

    /**
     * Finds all identifiers in the metadata database which are not deleted. Optionally, the result can be filtered by
     * database id and/or query id.
     *
     * @param type       The query type.
     * @param databaseId Optional. The database id.
     * @param queryId    Optional. The query id.
     * @param viewId     Optional. The view id.
     * @param tableId    Optional. The table id.
     * @return The list of identifiers.
     */
    List<Identifier> findAll(IdentifierTypeDto type, UUID databaseId, UUID queryId, UUID viewId, UUID tableId);

    /**
     * Publishes a draft identifier with DataCite.
     *
     * @param identifier The identifier.
     * @return The resulting identifier.
     * @throws SearchServiceException
     * @throws DatabaseNotFoundException
     * @throws SearchServiceConnectionException
     * @throws MalformedException
     * @throws DataServiceConnectionException
     */
    Identifier publish(Identifier identifier) throws SearchServiceException, DatabaseNotFoundException,
            SearchServiceConnectionException, MalformedException, DataServiceConnectionException,
            ExternalServiceException;

    /**
     * Creates a new identifier in the metadata database for a query or database.
     *
     * @param database The database.
     * @param user     The user.
     * @param data     The data.
     * @return The created identifier from the metadata database if successful.
     * @throws DataServiceException
     * @throws DataServiceConnectionException
     * @throws IdentifierNotFoundException
     * @throws MalformedException
     * @throws ViewNotFoundException
     * @throws DatabaseNotFoundException
     * @throws QueryNotFoundException
     * @throws SearchServiceException
     * @throws SearchServiceConnectionException
     */
    Identifier save(Database database, User user, IdentifierSaveDto data) throws DataServiceException,
            DataServiceConnectionException, IdentifierNotFoundException, MalformedException, ViewNotFoundException,
            DatabaseNotFoundException, QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException;

    /**
     * Creates a new identifier in the metadata database for a query or database.
     *
     * @param database The database.
     * @param user     The user.
     * @param data     The data.
     * @return The created identifier from the metadata database if successful.
     * @throws DataServiceException
     * @throws DataServiceConnectionException
     * @throws IdentifierNotFoundException
     * @throws MalformedException
     * @throws ViewNotFoundException
     * @throws DatabaseNotFoundException
     * @throws QueryNotFoundException
     * @throws SearchServiceException
     * @throws SearchServiceConnectionException
     */
    Identifier create(Database database, User user, CreateIdentifierDto data) throws DataServiceException,
            DataServiceConnectionException, IdentifierNotFoundException, MalformedException, ViewNotFoundException,
            DatabaseNotFoundException, QueryNotFoundException, SearchServiceException, SearchServiceConnectionException, ExternalServiceException;

    /**
     * Export metadata for a identifier
     *
     * @param identifier The identifier.
     * @return The export, if successful.
     */
    InputStreamResource exportMetadata(Identifier identifier);

    /**
     * Export metadata for bibliography for a identifier.
     *
     * @param identifier The identifier.
     * @param style      The identifier bibliography style. Optional. Default: APA.
     * @return The export, if successful.
     * @throws MalformedException The identifier style was not found.
     */
    String exportBibliography(Identifier identifier, BibliographyTypeDto style) throws MalformedException;

    /**
     * Exports an identifier to XML
     *
     * @param identifier The identifier.
     * @return The XML resource, if successful.
     * @throws DataServiceException
     * @throws DataServiceConnectionException
     * @throws IdentifierNotFoundException
     * @throws QueryNotFoundException
     */
    InputStreamResource exportResource(Identifier identifier) throws DataServiceException, DataServiceConnectionException,
            IdentifierNotFoundException, QueryNotFoundException;

    /**
     * Soft-deletes an identifier for a given id in the metadata database. Does not actually remove the entity from the
     * database, but sets it as deleted.
     *
     * @param identifier The identifier.
     * @throws DataServiceException
     * @throws DataServiceConnectionException
     * @throws IdentifierNotFoundException
     * @throws DatabaseNotFoundException
     * @throws SearchServiceException
     * @throws SearchServiceConnectionException
     */
    void delete(Identifier identifier) throws DataServiceException, DataServiceConnectionException, IdentifierNotFoundException,
            DatabaseNotFoundException, SearchServiceException, SearchServiceConnectionException;
}
