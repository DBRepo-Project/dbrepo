package at.tuwien.config;

import at.tuwien.api.user.UserDto;
import at.tuwien.mapper.UserMapper;
import at.tuwien.repository.elastic.UserIdxRepository;
import at.tuwien.repository.jpa.UserRepository;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Log4j2
public class IndexConfig {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final UserIdxRepository userIdxRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    @Autowired
    public IndexConfig(UserMapper userMapper, UserRepository userRepository,
                       UserIdxRepository userIdxRepository, ElasticsearchOperations elasticsearchOperations) {
        this.userMapper = userMapper;
        this.userRepository = userRepository;
        this.userIdxRepository = userIdxRepository;
        this.elasticsearchOperations = elasticsearchOperations;
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void initIndex() {
        final IndexCoordinates userIndex = IndexCoordinates.of("user");
        if (!elasticsearchOperations.indexOps(userIndex).exists()) {
            elasticsearchOperations.indexOps(userIndex).create();
            elasticsearchOperations.indexOps(userIndex).createMapping(UserDto.class);
            log.info("Created user index");
        }
        /* pre-fill */
        final List<UserDto> users = userRepository.findAll()
                .stream()
                .map(userMapper::userToUserDto)
                .toList();
        userIdxRepository.saveAll(users);
        log.info("Added {} users to OpenSearch index", users.size());
    }
}
