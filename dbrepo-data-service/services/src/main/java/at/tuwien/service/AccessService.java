package at.tuwien.service;

import at.tuwien.api.database.AccessTypeDto;
import at.tuwien.api.database.internal.PrivilegedDatabaseDto;
import at.tuwien.api.user.PrivilegedUserDto;
import at.tuwien.exception.*;

import java.sql.SQLException;

public interface AccessService {
    void create(PrivilegedDatabaseDto database, PrivilegedUserDto user, AccessTypeDto access) throws SQLException,
            DatabaseMalformedException;

    void update(PrivilegedDatabaseDto database, PrivilegedUserDto user, AccessTypeDto access) throws SQLException,
            DatabaseMalformedException;

    void delete(PrivilegedDatabaseDto database, PrivilegedUserDto user) throws SQLException,
            DatabaseMalformedException;
}
