package at.tuwien.service;

import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.exception.QueryMalformedException;

import java.sql.SQLException;

public interface SchemaService {

    TableDto obtainTableMetadata(PrivilegedDatabaseDto database, String tableName) throws SQLException,
            QueryMalformedException;
}
