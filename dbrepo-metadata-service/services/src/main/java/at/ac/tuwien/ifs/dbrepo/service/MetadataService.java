package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.user.external.ExternalMetadataDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiErrorType;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiListIdentifiersParameters;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiListRecordsParameters;
import at.ac.tuwien.ifs.dbrepo.oaipmh.OaiRecordParameters;
import org.springframework.transaction.annotation.Transactional;

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
     * Harvest records from a repository.
     *
     * @param parameters The list parameters.
     * @return The xml records listing.
     */
    String listRecords(OaiListRecordsParameters parameters);

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
     * Retrieve the set structure of a repository, useful for selective harvesting.
     *
     * @return The xml list of sets.
     */
    String listSets();

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
     * @throws OrcidNotFoundException          The provided identifier is of ORCID type and does not exist.
     * @throws RorNotFoundException            The provided identifier is of ROR type and does not exist.
     * @throws DoiNotFoundException            The doi was not found.
     * @throws IdentifierNotSupportedException The identifier is not supported.
     */
    ExternalMetadataDto findByUrl(String url) throws OrcidNotFoundException, RorNotFoundException,
            DoiNotFoundException, IdentifierNotSupportedException;
}
