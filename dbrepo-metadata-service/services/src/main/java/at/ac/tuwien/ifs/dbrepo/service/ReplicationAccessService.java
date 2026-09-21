package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationAccessDto;
import at.ac.tuwien.ifs.dbrepo.core.api.replication.ReplicationOwnerDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DataServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.exception.NotAllowedException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceConnectionException;
import at.ac.tuwien.ifs.dbrepo.core.exception.SearchServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;

import java.util.List;

public interface ReplicationAccessService {

    Database initialize(Database database, ReplicationOwnerDto owner) throws DataServiceException,
            DataServiceConnectionException, DatabaseNotFoundException, SearchServiceException,
            SearchServiceConnectionException;

    ReplicationAccessDto map(Database database, String localUsername) throws UserNotFoundException,
            NotAllowedException, DataServiceException, DataServiceConnectionException, DatabaseNotFoundException,
            SearchServiceException, SearchServiceConnectionException;

    List<ReplicationAccessDto> findPending();

}
