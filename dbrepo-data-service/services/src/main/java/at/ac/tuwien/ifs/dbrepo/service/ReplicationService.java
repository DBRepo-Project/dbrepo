package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.CreateDatabaseDto;

public interface ReplicationService {

    void replicateDatabase(CreateDatabaseDto createDatabaseDto);
}
