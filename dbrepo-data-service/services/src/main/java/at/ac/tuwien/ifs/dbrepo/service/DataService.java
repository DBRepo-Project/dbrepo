package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.api.Result;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseMalformedException;

import java.sql.SQLException;
import java.time.Instant;

public interface DataService {

    Result get(Database database, String tableOrViewName, Instant timestamp, Long page, Long size) throws SQLException, DatabaseMalformedException;
}
