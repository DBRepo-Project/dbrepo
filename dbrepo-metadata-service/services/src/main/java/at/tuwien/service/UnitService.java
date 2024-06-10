package at.tuwien.service;

import at.tuwien.entities.database.table.columns.TableColumnUnit;
import at.tuwien.exception.UnitNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UnitService {

    TableColumnUnit create(TableColumnUnit unit);

    /**
     * Finds all table column units in the metadata database.
     *
     * @return The list of table column units.
     */
    List<TableColumnUnit> findAll();

    /**
     * Finds a table column unit by given uri in the metadata database.
     *
     * @param uri The uri.
     * @return The table column unit, if successful.
     * @throws UnitNotFoundException The unit was not found.
     */
    TableColumnUnit find(String uri) throws UnitNotFoundException;

}
