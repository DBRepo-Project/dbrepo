package at.tuwien.service;

import at.tuwien.OaiErrorType;
import at.tuwien.OaiListIdentifiersParameters;
import at.tuwien.OaiRecordParameters;
import at.tuwien.exception.IdentifierNotFoundException;

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
