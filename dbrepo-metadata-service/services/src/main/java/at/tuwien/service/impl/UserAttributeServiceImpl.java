package at.tuwien.service.impl;

import at.tuwien.entities.user.UserAttribute;
import at.tuwien.exception.UserAttributeNotFoundException;
import at.tuwien.repository.mdb.UserAttributeRepository;
import at.tuwien.service.UserAttributeService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Log4j2
@Service
public class UserAttributeServiceImpl implements UserAttributeService {

    private final UserAttributeRepository userAttributeRepository;

    @Autowired
    public UserAttributeServiceImpl(UserAttributeRepository userAttributeRepository) {
        this.userAttributeRepository = userAttributeRepository;
    }

    @Override
    public UserAttribute find(UUID userId, String name) throws UserAttributeNotFoundException {
        final Optional<UserAttribute> optional = userAttributeRepository.findByUserIdAndName(userId, name);
        if (optional.isEmpty()) {
            log.error("Failed to find user attribute with name {}", name);
            throw new UserAttributeNotFoundException("Failed to find user attribute with name " + name);
        }
        return optional.get();
    }

    @Override
    public UserAttribute update(UUID userId, String name, String value) throws UserAttributeNotFoundException {
        final UserAttribute entity = find(userId, name);
        entity.setValue(value);
        return userAttributeRepository.save(entity);
    }

    @Override
    public UserAttribute create(UserAttribute userAttribute) {
        return userAttributeRepository.save(userAttribute);
    }
}
