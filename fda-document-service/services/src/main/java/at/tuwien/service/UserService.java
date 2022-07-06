package at.tuwien.service;

import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;

public interface UserService {
    User find(String username) throws UserNotFoundException;
}
