package at.tuwien.mapper;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.entities.user.User;
import org.mapstruct.Mapper;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface DatabaseMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DatabaseMapper.class);

    @Transactional(readOnly = true)
    default User containerToPrivilegedUser(Container container) {
        final String username = container.getImage()
                .getEnvironment()
                .stream()
                .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_USERNAME))
                .map(ContainerImageEnvironmentItem::getValue)
                .collect(Collectors.toList())
                .get(0);
        final String password = container.getImage()
                .getEnvironment()
                .stream()
                .filter(e -> e.getType().equals(ContainerImageEnvironmentItemType.PRIVILEGED_PASSWORD))
                .map(ContainerImageEnvironmentItem::getValue)
                .collect(Collectors.toList())
                .get(0);
        return User.builder()
                .username(username)
                .databasePassword(password)
                .build();
    }

}
