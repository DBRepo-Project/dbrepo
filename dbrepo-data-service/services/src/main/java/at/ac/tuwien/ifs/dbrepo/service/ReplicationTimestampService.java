package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.replication.TupleReplicationTimestampDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;

import java.sql.SQLException;
import java.util.List;

public interface ReplicationTimestampService {

    void saveTimestamps(Database database, List<TupleReplicationTimestampDto> timestamps) throws SQLException;

    void closeAndSaveTimestamps(Database database, List<TupleReplicationTimestampDto> timestamps) throws SQLException;

    void updateTimestampRowEnds(Database database, List<TupleReplicationTimestampDto> timestamps) throws SQLException;
}
