package at.tuwien.service;

import at.tuwien.entities.user.UserAttribute;
import at.tuwien.exception.UserAttributeNotFoundException;

import java.util.UUID;

public interface UserAttributeService {

    UserAttribute find(UUID userId, String name) throws UserAttributeNotFoundException;

    UserAttribute update(UUID userId, String name, String value) throws UserAttributeNotFoundException;

    UserAttribute create(UserAttribute userAttribute);
}
