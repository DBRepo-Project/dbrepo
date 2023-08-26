package at.tuwien.service.impl;

import at.tuwien.api.auth.SignupRequestDto;
import at.tuwien.api.user.*;
import at.tuwien.exception.*;
import at.tuwien.gateway.KeycloakGateway;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.sdb.UserIdxRepository;
import at.tuwien.service.UserService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Log4j2
@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final KeycloakGateway keycloakGateway;
    private final UserIdxRepository userIdxRepository;

    @Autowired
    public UserServiceImpl(UserMapper userMapper, KeycloakGateway keycloakGateway,
                           UserIdxRepository userIdxRepository) {
        this.userMapper = userMapper;
        this.keycloakGateway = keycloakGateway;
        this.userIdxRepository = userIdxRepository;
    }

    @Override
    public List<UserBriefDto> findAll() throws KeycloakRemoteException, AccessDeniedException {
        return keycloakGateway.findAllUsers()
                .stream()
                .map(userMapper::keycloakUserDtoToUserBriefDto)
                .toList();
    }

    @Override
    public UserDto findByUsername(String username) throws UserNotFoundException, KeycloakRemoteException,
            AccessDeniedException {
        return userMapper.keycloakUserDtoToUserDto(keycloakGateway.findByUsername(username));
    }

    @Override
    public UserDto find(UUID id) throws UserNotFoundException, KeycloakRemoteException, AccessDeniedException {
        return userMapper.keycloakUserDtoToUserDto(keycloakGateway.findById(id));
    }

    @Override
    public UserDto create(SignupRequestDto data) throws UserAlreadyExistsException, AccessDeniedException,
            KeycloakRemoteException, UserNotFoundException {
        /* create */
        keycloakGateway.createUser(userMapper.signupRequestDtoToUserCreateDto(data));
        final at.tuwien.api.keycloak.UserDto keycloakUser = keycloakGateway.findByUsername(data.getUsername());
        final UserDto userDto = userMapper.keycloakUserDtoToUserDto(keycloakUser);
        /* save in open search database */
        userIdxRepository.save(userMapper.keycloakUserDtoToUserDto(keycloakUser));
        log.info("Created user with id {} in open search database", userDto.getId());
        return userDto;
    }

    @Override
    public UserDto modify(UUID id, UserUpdateDto data) throws UserNotFoundException, UserAttributeNotFoundException,
            KeycloakRemoteException, AccessDeniedException {
        /* save */
        keycloakGateway.updateUserAttributes(id, userMapper.userUpdateDtoToUserAttributesDto(data));
        log.info("Updated user attributes for user with id {}", id);
        /* save in open search database */
        final UserDto user = userMapper.keycloakUserDtoToUserDto(keycloakGateway.findById(id));
        userIdxRepository.save(user);
        return user;
    }

    @Override
    public void updatePassword(UUID id, UserPasswordDto data) throws KeycloakRemoteException, AccessDeniedException,
            UserNotFoundException {
        /* save */
        keycloakGateway.updateUserCredentials(id, data);
        log.info("Updated user password with id {}", id);
    }

    @Override
    public UserDto toggleTheme(UUID id, UserThemeSetDto data) throws UserNotFoundException, KeycloakRemoteException,
            AccessDeniedException {
        /* save */
        keycloakGateway.updateUserAttributes(id, userMapper.userThemeSetDtoToUserAttributesDto(data));
        log.info("Updated theme by updating attribute with id {}", id);
        return userMapper.keycloakUserDtoToUserDto(keycloakGateway.findById(id));
    }

    @Override
    public void validateUsernameNotExists(String username) throws UserAlreadyExistsException {
        try {
            keycloakGateway.findByUsername(username);
        } catch (KeycloakRemoteException | AccessDeniedException e) {
            log.error("User with username {} already exists", username);
            throw new UserAlreadyExistsException("User with username " + username + " already exists");
        } catch (UserNotFoundException e) {
            /* ignore */
        }
    }

    @Override
    public void validateEmailNotExists(String email) throws UserEmailAlreadyExistsException {
        try {
            keycloakGateway.findByEmail(email);
        } catch (KeycloakRemoteException | AccessDeniedException e) {
            log.error("User with email {} already exists", email);
            throw new UserEmailAlreadyExistsException("User with email " + email + " already exists");
        } catch (UserNotFoundException e) {
            /* ignore */
        }
    }
}
