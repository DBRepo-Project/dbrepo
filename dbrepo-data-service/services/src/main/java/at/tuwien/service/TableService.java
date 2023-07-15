package at.tuwien.service;

import at.tuwien.api.database.table.TableBriefDto;
import at.tuwien.api.database.table.TableDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.ColumnTypeMalformedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;

import java.util.List;

public interface TableService {

    List<TableBriefDto> findAll(Database database) throws QueryMalformedException;

    TableDto find(Database database, String name) throws TableNotFoundException, ColumnTypeMalformedException;

    Table save(TableDto data);
}
