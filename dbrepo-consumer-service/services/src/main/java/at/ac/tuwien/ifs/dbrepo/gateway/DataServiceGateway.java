package at.ac.tuwien.ifs.dbrepo.gateway;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.RemoteUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.TableNotFoundException;

import java.util.UUID;

public interface DataServiceGateway {

    void insertRawTuple(UUID databaseId, UUID tableId, TupleDto tuple) throws RemoteUnavailableException,
            TableNotFoundException, DataServiceException;
}
