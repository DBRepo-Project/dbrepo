package at.tuwien.service;

import at.tuwien.entities.user.User;

import java.util.List;

public interface UserService {

    /**
     * Finds all users from the metadata database.
     *
     * @return List of users.
     */
    List<User> findAll();
}
