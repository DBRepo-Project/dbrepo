package at.tuwien.service;

import at.tuwien.exception.IdentifierNotFoundException;
import at.tuwien.oaipmh.OaiErrorType;
import at.tuwien.oaipmh.OaiListIdentifiersParameters;
import at.tuwien.oaipmh.OaiRecordParameters;

public interface MetadataService {

    /**
     * @return
     */
    String identify();

    String listIdentifiers(OaiListIdentifiersParameters parameters);

    String getRecord(OaiRecordParameters parameters) throws IdentifierNotFoundException;

    String listMetadataFormats();

    String error(OaiErrorType type);
}
