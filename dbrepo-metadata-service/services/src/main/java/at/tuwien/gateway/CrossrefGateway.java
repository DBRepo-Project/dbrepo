package at.tuwien.gateway;

import at.tuwien.api.crossref.CrossrefDto;
import at.tuwien.exception.DoiNotFoundException;

public interface CrossrefGateway {

    /**
     * Retrieves metadata from the CrossRef funder database for a given CrossRef id.
     *
     * @param id The CrossRef id.
     * @return The CrossRef metadata from the CrossRef funder database.
     * @throws DoiNotFoundException The metadata was not found in the CrossRef funder database.
     */
    CrossrefDto findById(String id) throws DoiNotFoundException;
}
