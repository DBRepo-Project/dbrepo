package at.tuwien.service;

import at.tuwien.api.identifier.*;
import at.tuwien.ExportResource;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.exception.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;

@Service
public interface IdentifierService {

    /**
     * Finds all identifiers in the metadata database which are not deleted.
     *
     * @param queryId    Optional. The query id.
     * @param databaseId Optional. The database id.
     * @return List of identifiers
     */
    List<Identifier> findAll(Long databaseId, Long queryId);

    /**
     * Finds all identifiers in the metadata database which are not deleted and filter by query id.
     *
     * @param containerId The container id.
     * @param databaseId  The database id.
     * @param queryId     The query id.
     * @return The identifier, if found.
     * @throws IdentifierNotFoundException No identifier with the query id was found.
     */
    Identifier find(Long containerId, Long databaseId, Long queryId) throws IdentifierNotFoundException;

    /**
     * Finds all identifiers
     *
     * @return List of identifiers.
     */
    List<Identifier> findAll();

    /**
     * Creates a new identifier in the metadata database which is not yet published
     *
     * @param data          The identifier.
     * @param principal     The authorization principal.
     * @param authorization The authorization bearer.
     * @return The created identifier from the metadata database if successful.
     */
    Identifier create(IdentifierCreateDto data, Principal principal, String authorization)
            throws IdentifierPublishingNotAllowedException, QueryNotFoundException,
            RemoteUnavailableException, IdentifierAlreadyExistsException, UserNotFoundException,
            DatabaseNotFoundException;

    /**
     * Finds an identifier by given id in the metadata database.
     *
     * @param identifierId The identifier id.
     * @return The found identifier from the metadata database if successful.
     * @throws IdentifierNotFoundException The identifier was not found in the metadata database or was deleted.
     */
    Identifier find(Long identifierId) throws IdentifierNotFoundException;

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
     * @param style The identifier bibliography style.
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
     * @throws RemoteUnavailableException  The remote service is not available
     */
    InputStreamResource exportResource(Long identifierId)
            throws IdentifierNotFoundException, QueryNotFoundException, RemoteUnavailableException, IdentifierRequestException;

    /**
     * Updated the metadata (only) on the identifier for a given id in the metadata database.
     *
     * @param identifierId The identifier id.
     * @param data         The metadata.
     * @return The updated identifier if successful.
     * @throws IdentifierNotFoundException             TThe identifier was not found in the metadata database or was deleted.
     * @throws IdentifierPublishingNotAllowedException The identifier contained a visibility change which is not allowed here.
     */
    Identifier update(Long identifierId, IdentifierDto data) throws IdentifierNotFoundException,
            IdentifierPublishingNotAllowedException;

    /**
     * Publishes the identifier for a given identifier id in the metadata database.
     *
     * @param identifierId The identifier id.
     * @param visibility   The new visibility.
     * @return The updated identifier from the metadata database.
     * @throws IdentifierNotFoundException         The identifier was not found in the metadata database or was deleted.
     * @throws IdentifierAlreadyPublishedException The identifier is already published (=EVERYONE) and cannot be un-published.
     */
    Identifier publish(Long identifierId, VisibilityTypeDto visibility)
            throws IdentifierNotFoundException,
            IdentifierAlreadyPublishedException;

    /**
     * Soft-deletes an identifier for a given id in the metadata database. Does not actually remove the entity from the database, but sets it as deleted.
     *
     * @param identifierId The identifier id.
     * @throws IdentifierNotFoundException The identifier was not found in the metadata database or was deleted.
     */
    void delete(Long identifierId) throws IdentifierNotFoundException;
}
