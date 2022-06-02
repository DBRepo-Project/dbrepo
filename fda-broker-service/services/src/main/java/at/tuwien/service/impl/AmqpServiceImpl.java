package at.tuwien.service.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.exception.ProcessCompletionException;
import at.tuwien.service.QueueService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AmqpServiceImpl implements QueueService {

    private final static Runtime RUNTIME = Runtime.getRuntime();

    @Override
    public void createUser(CreateUserDto data) throws ProcessCompletionException {
        final String addUserCmd = "rabbitmqctl add_user " + data.getUsername() + " " + data.getPassword();
        final String setUserTagsCmd = "rabbitmqctl set_user_tags " + data.getUsername() + " administrator";
        final String setPermissionsCmd = "rabbitmqctl set_permissions -p / " + data.getUsername() + " \".*' \".*\" " +
                "\".*\"";
        try {
            RUNTIME.exec(addUserCmd)
                    .waitFor(3, TimeUnit.SECONDS);
            RUNTIME.exec(setUserTagsCmd)
                    .waitFor(3, TimeUnit.SECONDS);
            RUNTIME.exec(setPermissionsCmd)
                    .waitFor(3, TimeUnit.SECONDS);
        } catch (IOException e) {
            log.error("Failed to execute process");
            throw new ProcessCompletionException("Failed to execute process", e);
        } catch (InterruptedException e) {
            log.error("Failed to wait for process");
            throw new ProcessCompletionException("Failed to wait for process", e);
        }
    }

}
