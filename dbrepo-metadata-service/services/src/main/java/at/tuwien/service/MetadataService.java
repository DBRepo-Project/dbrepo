package at.tuwien.service;

import at.tuwien.exception.IdentifierNotFoundException;
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
}
