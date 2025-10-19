package at.ac.tuwien.ifs.dbrepo.service.impl;

import at.ac.tuwien.ifs.dbrepo.core.api.user.UserDto;
import at.ac.tuwien.ifs.dbrepo.core.api.user.UserUpdateDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AuthServiceException;
import at.ac.tuwien.ifs.dbrepo.core.exception.UserNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.gateway.KeycloakGateway;
import at.ac.tuwien.ifs.dbrepo.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private final MetadataMapper metadataMapper;
    private final KeycloakGateway keycloakGateway;

    @Autowired
    public UserServiceImpl(MetadataMapper metadataMapper, KeycloakGateway keycloakGateway) {
        this.metadataMapper = metadataMapper;
        this.keycloakGateway = keycloakGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return keycloakGateway.findAll()
                .stream()
                .map(metadataMapper::userRepresentationToUserDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findByUsername(String username) throws UserNotFoundException {
        return metadataMapper.userRepresentationToUserDto(keycloakGateway.findByUsername(username));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDto findById(UUID id) throws UserNotFoundException {
        return metadataMapper.userRepresentationToUserDto(keycloakGateway.findById(id));
    }

    @Override
    @Transactional
    public UserDto modify(UserDto user, UserUpdateDto data) throws UserNotFoundException, AuthServiceException {
        user.setFirstname(data.getFirstname());
        user.setLastname(data.getLastname());
        user.getAttributes()
                .setAffiliation(data.getAffiliation());
        user.getAttributes()
                .setOrcid(data.getOrcid());
        user.getAttributes()
                .setTheme(data.getTheme());
        user.getAttributes()
                .setLanguage(data.getLanguage());
        /* save in auth service */
        keycloakGateway.updateUser(user.getId(), data);
        log.info("Modified user with id: {}", user.getId());
        return user;
    }
}
