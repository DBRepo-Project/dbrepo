package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.datacite.doi.DataCiteDoiEvent;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.ExternalServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.MalformedException;

public interface DataCiteGateway {

    /**
     * Saves the PID remotely in DataCite Fabrica
     *
     * @return The DOI for this PID.
     * @throws MalformedException
     */
    String create() throws MalformedException, ExternalServiceException;

    /**
     * Saves the PID remotely in DataCite Fabrica
     *
     * @param identifier The identifier information
     * @param event The event.
     * @return The DOI for this PID.
     * @throws MalformedException
     * @throws DataServiceConnectionException
     * @throws ExternalServiceException
     */
    String save(Identifier identifier, DataCiteDoiEvent event) throws MalformedException, ExternalServiceException;
}
