package at.tuwien.service;

import at.tuwien.entities.database.table.columns.TableColumnUnit;

import java.util.List;

public interface UnitService {

    /**
     * Finds all column units in the metadata database.
     *
     * @return List of column units.
     */
    List<TableColumnUnit> findAll();
}
