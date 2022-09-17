package at.tuwien.service;

import at.tuwien.OaiErrorType;
import at.tuwien.OaiListIdentifiersParameters;

public interface MetadataService {

    /**
     * @return
     */
    String identify();

    String listIdentifiers(OaiListIdentifiersParameters parameters);

    String error(OaiErrorType type);
}
