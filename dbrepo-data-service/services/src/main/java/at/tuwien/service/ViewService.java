package at.tuwien.service;

import at.tuwien.api.database.ViewDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.ColumnTypeMalformedException;
import at.tuwien.exception.QueryMalformedException;
import at.tuwien.exception.ViewNotFoundException;

import java.util.List;

public interface ViewService {

    List<ViewDto> findAll(Database database) throws QueryMalformedException;

    ViewDto find(Database database, String name) throws ViewNotFoundException, ColumnTypeMalformedException;
}
