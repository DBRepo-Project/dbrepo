package at.tuwien.service;

import at.tuwien.api.user.UserBriefDto;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import at.tuwien.exception.UserNotFoundException;

import java.util.List;

public interface UserService {

    List<UserBriefDto> findAll(Database database) throws UserNotFoundException;

    User save(UserBriefDto data);
}
