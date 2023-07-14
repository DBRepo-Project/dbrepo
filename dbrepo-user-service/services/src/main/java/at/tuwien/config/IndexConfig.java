package at.tuwien.config;

import at.tuwien.api.user.UserDto;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.sdb.UserIdxRepository;
import at.tuwien.repository.mdb.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Log4j2
public class IndexConfig {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final UserIdxRepository userIdxRepository;

    @Autowired
    public IndexConfig(UserMapper userMapper, UserRepository userRepository, UserIdxRepository userIdxRepository) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.userIdxRepository = userIdxRepository;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final List<UserDto> users = userRepository.findAll()
                .stream()
                .map(userMapper::userToUserDto)
                .toList();
        userIdxRepository.saveAll(users);
        log.info("Added {} users to open search database", users.size());
    }
}
