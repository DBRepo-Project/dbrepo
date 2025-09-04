package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;

import java.util.Map;

public interface ReplicationService {

    void replicateDatabase(CreateDatabaseDto createDatabaseDto);

    void replicateTuple(TupleWithTimestampsDto tupleWithTimestamps, DatabaseDto database, TableDto table);
    
    void replicateTupleDelete(TupleWithTimestampsDto tupleWithTimestamps, DatabaseDto database, TableDto table);
    
    void replicateQuery(DatabaseDto database, QueryDto query);
}
