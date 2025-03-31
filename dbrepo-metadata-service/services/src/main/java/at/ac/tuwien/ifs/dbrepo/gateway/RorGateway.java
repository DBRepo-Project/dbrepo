package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.ror.RorDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.RorNotFoundException;

public interface RorGateway {

    /**
     * Retrieves metadata from the ROR database for an organizational ROR id.
     *
     * @param id The ROR id.
     * @return The metadata from the ROR database, if successful.
     * @throws RorNotFoundException The ROR id was not found in the ROR database.
     */
    RorDto findById(String id) throws RorNotFoundException;
}
