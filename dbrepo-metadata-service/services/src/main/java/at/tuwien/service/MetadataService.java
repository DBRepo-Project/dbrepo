package at.tuwien.service;

import at.tuwien.api.user.external.ExternalMetadataDto;
import at.tuwien.exception.*;
import at.tuwien.oaipmh.OaiErrorType;
import at.tuwien.oaipmh.OaiListIdentifiersParameters;
import at.tuwien.oaipmh.OaiRecordParameters;

public interface MetadataService {

    /**
     * Get identification information of the repository.
     *
     * @return The xml identification information.
     */
    String identify();

    /**
     * Get a list of all identifiers in the repository.
     *
     * @param parameters The list parameters.
     * @return The xml identifier listing.
     */
    String listIdentifiers(OaiListIdentifiersParameters parameters);

    /**
     * Get a record.
     *
     * @param parameters The parameters.
     * @return The xml record.
     * @throws IdentifierNotFoundException The identifier was not found.
     */
    String getRecord(OaiRecordParameters parameters) throws IdentifierNotFoundException;

    /**
     * Get a list of metadata formats.
     *
     * @return The xml list of metadata formats available.
     */
    String listMetadataFormats();

    /**
     * Produce an error, this method is used as a wrapper function.
     *
     * @param type The error type.
     * @return The xml error.
     */
    String error(OaiErrorType type);

    /**
     * Finds creator user metadata by remote service and user identifier.
     *
     * @param url The user identifier.
     * @return The user metadata.
     * @throws OrcidNotFoundException      The provided identifier is of ORCID type and does not exist.
     * @throws RorNotFoundException        The provided identifier is of ROR type and does not exist.
     * @throws IdentifierNotFoundException The identifier is not supported.
     * @throws DoiNotFoundException        The doi was not found.
     */
    ExternalMetadataDto findByUrl(String url) throws OrcidNotFoundException, RorNotFoundException,
            DoiNotFoundException, IdentifierNotFoundException;
}
