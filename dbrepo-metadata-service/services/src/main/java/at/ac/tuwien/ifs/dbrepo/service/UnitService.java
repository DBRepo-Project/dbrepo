package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumnUnit;
import at.ac.tuwien.ifs.dbrepo.core.exception.UnitNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface UnitService {

    /**
     * Creates a table column unit in the metadata database.
     *
     * @param unit The table column unit.
     * @return The saved table column unit, if successful.
     */
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
