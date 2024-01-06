package at.tuwien.service;

import at.tuwien.api.identifier.BibliographyTypeDto;
import at.tuwien.api.identifier.IdentifierSaveDto;
import at.tuwien.api.identifier.IdentifierTypeDto;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;

import java.security.Principal;
import java.util.List;

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
    List<Identifier> findAll(Long databaseId);

    /**
     * Finds an identifier by given id.
     *
     * @param id The identifier id.
     * @return The identifier.
     * @throws IdentifierNotFoundException The identifier does not exist.
     */
    Identifier find(Long id) throws IdentifierNotFoundException;

    Identifier findByDoi(String doi) throws IdentifierNotFoundException;

    /**
     * Finds all identifiers in the metadata database which are not deleted and filter by query id.
     *
     * @param databaseId The database id.
     * @param queryId    The query id.
     * @return The identifiers, if found.
     */
    List<Identifier> findByDatabaseIdAndQueryId(Long databaseId, Long queryId);

    List<Identifier> findAllDatabaseIdentifiers();

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
     * @return List of identifiers
     */
    List<Identifier> findAll(IdentifierTypeDto type, Long databaseId, Long queryId, Long viewId, Long tableId);

    /**
     * Creates a new identifier in the metadata database for a query or database.
     *
     * @param data      The identifier.
     * @param principal The authorization principal.
     * @return The created identifier from the metadata database if successful.
     * @throws IdentifierPublishingNotAllowedException The identifier with this visibility could not be created.
     * @throws QueryNotFoundException                  The query was not found in the data database.
     * @throws RemoteUnavailableException              The connection to the Query Store could not be established by
     *                                                 the database connector.
     * @throws IdentifierAlreadyExistsException        The identifier for this query/database already exists.
     * @throws UserNotFoundException                   The user was not found in the metadata database.
     * @throws DatabaseNotFoundException               The database was not found in the metadata database.
     * @throws IdentifierNotFoundException             The identifier was not found in the metadata database.
     */
    Identifier create(IdentifierSaveDto data, Principal principal) throws IdentifierPublishingNotAllowedException,
            QueryNotFoundException, RemoteUnavailableException, IdentifierAlreadyExistsException, UserNotFoundException,
            DatabaseNotFoundException, IdentifierRequestException, ViewNotFoundException, QueryStoreException,
            DatabaseConnectionException, ImageNotSupportedException, IdentifierNotFoundException;

    /**
     * Export metadata for a identifier
     *
     * @param id The identifier id.
     * @return The export, if successful.
     * @throws IdentifierNotFoundException The identifier was not found in the metadata database or was deleted.
     */
    InputStreamResource exportMetadata(Long id) throws IdentifierNotFoundException;

    /**
     * Export metadata for bibliography for a identifier.
     *
     * @param id    The identifier id.
     * @param style The identifier bibliography style. Optional. Default: APA.
     * @return The export, if successful.
     * @throws IdentifierNotFoundException The identifier was not found in the metadata database or was deleted.
     * @throws IdentifierRequestException  The identifier style was not found.
     */
    String exportBibliography(Long id, BibliographyTypeDto style) throws IdentifierNotFoundException, IdentifierRequestException;

    /**
     * Exports an identifier to XML
     *
     * @param identifierId The identifier id.
     * @return The XML resource, if successful.
     * @throws IdentifierNotFoundException The identifier was not found in the metadata database or was deleted.
     * @throws QueryNotFoundException      The query was not found in the metadata database or was deleted.
     * @throws IdentifierRequestException  The identifier does not allow for exporting.
     */
    InputStreamResource exportResource(Long identifierId, Principal principal) throws IdentifierNotFoundException, QueryNotFoundException, FileStorageException, IdentifierRequestException, UserNotFoundException, QueryStoreException, TableMalformedException, DatabaseConnectionException, QueryMalformedException, DatabaseNotFoundException, ImageNotSupportedException, DataDbSidecarException;

    /**
     * Soft-deletes an identifier for a given id in the metadata database. Does not actually remove the entity from the
     * database, but sets it as deleted.
     *
     * @param identifierId The identifier id.
     * @throws IdentifierNotFoundException The identifier was not found in the metadata database or was deleted.
     */
    void delete(Long identifierId) throws IdentifierNotFoundException, DatabaseNotFoundException;
}
