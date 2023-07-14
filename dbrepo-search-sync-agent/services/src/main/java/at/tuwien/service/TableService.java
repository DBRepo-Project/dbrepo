package at.tuwien.service;

import at.tuwien.entities.database.table.Table;

import java.util.List;

public interface TableService {

    /**
     * Select all tables from the metadata database.
     *
     * @return The list of tables.
     */
    List<Table> findAll();
}
