package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TableDto;

import java.util.Map;

public interface ReplicationService {

    void replicateDatabase(CreateDatabaseDto createDatabaseDto);

    void replicateTuple(Map<String, Object> tupleWithTimestamps, DatabaseDto database, TableDto table);
}
