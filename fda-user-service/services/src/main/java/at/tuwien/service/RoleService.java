package at.tuwien.service;

import at.tuwien.entities.user.Role;
import at.tuwien.exception.RoleNotFoundException;

public interface RoleService {
    Role find(String name) throws RoleNotFoundException;
}
