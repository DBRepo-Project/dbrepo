package at.tuwien.service;

import at.tuwien.api.container.internal.PrivilegedContainerDto;
import at.tuwien.api.database.internal.CreateDatabaseDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.user.internal.UpdateUserPasswordDto;
import at.tuwien.exception.DatabaseMalformedException;

import java.sql.SQLException;

public interface DatabaseService {

    PrivilegedDatabaseDto create(PrivilegedContainerDto container, CreateDatabaseDto data) throws SQLException,
            DatabaseMalformedException;

    void update(PrivilegedDatabaseDto database, UpdateUserPasswordDto data) throws SQLException,
            DatabaseMalformedException;
}
