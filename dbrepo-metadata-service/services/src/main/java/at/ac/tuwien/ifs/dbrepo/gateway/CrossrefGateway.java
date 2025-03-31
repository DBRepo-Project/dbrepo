package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.crossref.CrossrefDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DoiNotFoundException;

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
