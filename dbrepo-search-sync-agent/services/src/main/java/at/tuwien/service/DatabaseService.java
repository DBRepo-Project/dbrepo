package at.tuwien.service;

import at.tuwien.entities.database.Database;

import java.util.List;

public interface DatabaseService {

    /**
     * Finds all databases in the metadata database.
     *
     * @return List of databases.
     */
    List<Database> findAll();
}
