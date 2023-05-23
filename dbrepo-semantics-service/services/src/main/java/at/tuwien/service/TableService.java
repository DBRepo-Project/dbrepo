package at.tuwien.service;

import at.tuwien.api.semantics.TableColumnEntityDto;
import at.tuwien.entities.database.table.Table;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.TableNotFoundException;

import java.util.List;

public interface TableService {
    Table find(Long databaseId, Long tableId) throws TableNotFoundException;

    List<TableColumnEntityDto> suggest(Long databaseId, Long tableId) throws TableNotFoundException, QueryMalformedException;
}
