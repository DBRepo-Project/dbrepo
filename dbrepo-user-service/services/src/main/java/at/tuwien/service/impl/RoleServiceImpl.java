package at.tuwien.service.impl;

import at.tuwien.entities.user.Role;
import at.tuwien.exception.RoleNotFoundException;
import at.tuwien.repository.jpa.RoleRepository;
import at.tuwien.service.RoleService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Log4j2
@Service
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;

    @Autowired
    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role find(String name) throws RoleNotFoundException {
        final Optional<Role> optional = roleRepository.findByName(name);
        if (optional.isEmpty()) {
            log.error("Failed to find role with name {}", name);
            throw new RoleNotFoundException("Failed to find role with name " + name);
        }
        final Role role = optional.get();
        log.trace("found role {}", role);
        return role;
    }

}
