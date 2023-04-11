package at.tuwien.service;

import at.tuwien.entities.user.UserAttribute;
import at.tuwien.exception.UserAttributeNotFoundException;

import java.util.List;

public interface UserAttributeService {
    List<UserAttribute> findAll(String userId);

    UserAttribute find(String userId, String name) throws UserAttributeNotFoundException;

    UserAttribute update(String userId, String name, String value) throws UserAttributeNotFoundException;

    UserAttribute create(UserAttribute userAttribute);
}
