package at.tuwien.mapper;

import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItem;
import at.tuwien.entities.container.image.ContainerImageEnvironmentItemType;
import at.tuwien.exception.ImageNotSupportedException;
import org.mapstruct.Mapper;

import java.util.Optional;
import java.util.Properties;

@Mapper(componentModel = "spring")
public interface ImageMapper {

    org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ImageMapper.class);

    @Deprecated
    default Properties containerImageToProperties(ContainerImage data) throws ImageNotSupportedException {
        final Properties properties = new Properties();
        final Optional<ContainerImageEnvironmentItem> username = data.getEnvironment()
                .stream()
                .filter(i -> i.getType().equals(ContainerImageEnvironmentItemType.USERNAME))
                .findFirst();
        if (username.isEmpty()) {
            log.error("Credentials error: no username found");
            throw new ImageNotSupportedException("Credentials error");
        }
        final Optional<ContainerImageEnvironmentItem> password = data.getEnvironment()
                .stream()
                .filter(i -> i.getType().equals(ContainerImageEnvironmentItemType.PASSWORD))
                .findFirst();
        if (password.isEmpty()) {
            log.error("Credentials error: no password found");
            throw new ImageNotSupportedException("Credentials error");
        }
        properties.setProperty("user", username.get()
                .getValue());
        properties.setProperty("password", password.get()
                .getValue());
        return properties;
    }

}
