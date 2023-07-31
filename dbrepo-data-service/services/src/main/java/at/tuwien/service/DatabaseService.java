package at.tuwien.service;

import at.tuwien.api.database.DatabaseBriefDto;
import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.exception.ContainerNotFoundException;
import at.tuwien.exception.DatabaseNotFoundException;

import java.util.List;

public interface DatabaseService {

    List<DatabaseBriefDto> findAll(Container container) throws DatabaseNotFoundException;

    Database save(DatabaseBriefDto data) throws ContainerNotFoundException;
}
