package at.tuwien.service;

import at.tuwien.api.database.ViewDto;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.QueryMalformedException;

import java.util.List;

public interface ViewService {

    List<ViewDto> findAll(Database database) throws QueryMalformedException;
}
