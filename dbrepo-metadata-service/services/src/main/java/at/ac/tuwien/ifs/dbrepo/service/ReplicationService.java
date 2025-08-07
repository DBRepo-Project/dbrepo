package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.CreateTableDto;

import java.util.UUID;

public interface ReplicationService {

    void replicateDatabase(CreateDatabaseDto createDatabaseDto, UUID creationId);
    
    void replicateTable(CreateTableDto createTableDto, UUID databaseId);
}