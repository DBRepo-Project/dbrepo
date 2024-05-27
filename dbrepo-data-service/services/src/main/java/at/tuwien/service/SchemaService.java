package at.tuwien.service;

import at.tuwien.api.database.ViewDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.exception.*;

import java.sql.SQLException;

public interface SchemaService {

    TableDto inspectTable(PrivilegedDatabaseDto database, String tableName) throws SQLException,
            QueryMalformedException, TableNotFoundException;

    ViewDto inspectView(PrivilegedDatabaseDto database, String viewName) throws SQLException, ViewMalformedException, ViewNotFoundException, ViewSchemaException;
}
