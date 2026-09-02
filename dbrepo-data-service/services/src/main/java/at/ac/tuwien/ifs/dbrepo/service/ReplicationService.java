package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleWithTimestampsDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Table;

public interface ReplicationService {

    void replicateTuple(TupleWithTimestampsDto tuple, Database database, Table table);

    void replicateTupleUpdate(TupleWithTimestampsDto tuple, Database database, Table table);

    void replicateTupleDelete(TupleWithTimestampsDto tuple, Database database, Table table);
}
