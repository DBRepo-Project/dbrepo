package at.tuwien.service;

import at.tuwien.OaiErrorType;
import at.tuwien.OaiListIdentifiersParameters;
import at.tuwien.OaiRecordParameters;
import at.tuwien.exception.IdentifierNotFoundException;

public interface MetadataService {

    /**
     * Identifies the repository.
     *
     * @return The xml response of identifying the database repository instance.
     */
    String identify();

    /**
     * List all identifiers known in the metadata database.
     *
     * @param parameters The filtering parameters.
     * @return The xml response of all identifiers.
     */
    String listIdentifiers(OaiListIdentifiersParameters parameters);

    /**
     * Finds a specific record for a given parameter in the metadata database.
     *
     * @param parameters The filtering parameters.
     * @return The xml response of a specific record.
     * @throws IdentifierNotFoundException THe identifier was not found in the metadata database.
     */
    String getRecord(OaiRecordParameters parameters) throws IdentifierNotFoundException;

    /**
     * List all metadata formats supported in the database repository.
     *
     * @return List of metadata formats.
     */
    String listMetadataFormats();

    /**
     * Internal function to produce an OAI-PMH error message.
     *
     * @param type The error type.
     * @return The error.
     */
    String error(OaiErrorType type);
}
