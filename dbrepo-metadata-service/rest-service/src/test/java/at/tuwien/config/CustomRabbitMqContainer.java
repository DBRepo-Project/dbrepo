package at.tuwien.config;

import org.testcontainers.containers.RabbitMQContainer;

public class CustomRabbitMqContainer extends RabbitMQContainer {

    public CustomRabbitMqContainer(String dockerImageName) {
        super(dockerImageName);
        super.addFixedExposedPort(5672, 5672);
        super.addFixedExposedPort(15672, 15672);
    }

}
