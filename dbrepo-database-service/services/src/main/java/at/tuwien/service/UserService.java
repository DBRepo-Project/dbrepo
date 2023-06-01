package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;

import java.util.UUID;

public interface UserService {

    User findByUsername(String username) throws UserNotFoundException;

    User find(UUID id) throws UserNotFoundException;
}
