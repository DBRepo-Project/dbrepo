package at.tuwien.service.impl;

import at.tuwien.api.amqp.CreateUserDto;
import at.tuwien.api.amqp.CreateVirtualHostDto;
import at.tuwien.api.amqp.GrantComponentDto;
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
        final StringBuilder addUserCmd = new StringBuilder("rabbitmqctl add_user ")
                .append(data.getUsername())
                .append(" ")
                .append(data.getPassword());
        executeSync(addUserCmd.toString());
    }

    @Override
    public void createVirtualHost(CreateVirtualHostDto data) throws ProcessCompletionException {
        final StringBuilder createVirtualHostCmd = new StringBuilder("rabbitmqctl add_vhost ")
                .append(data.getName());
        if (data.getDescription() != null) {
            createVirtualHostCmd.append(" --description \"")
                    .append(data.getDescription())
                    .append("\"");
        }
        if (data.getTags() != null) {
            createVirtualHostCmd.append(" --tags \"")
                    .append(data.getTags())
                    .append("\"");
        }
        executeSync(createVirtualHostCmd.toString());
    }

    @Override
    public void grantVirtualHost(GrantComponentDto data) throws ProcessCompletionException {
        final StringBuilder setPermissionsCmd = new StringBuilder("rabbitmqctl set_permissions -p / ")
                .append(data.getUsername())
                .append(" ")
                .append(data.getName())
                .append(" ")
                .append(data.getName())
                .append(" ")
                .append(data.getName());
        executeSync(setPermissionsCmd.toString());
    }

    /**
     * Executes a synchronous command line command
     *
     * @param cmd The command.
     * @throws ProcessCompletionException The synchronized waiting for the command to complete failed
     */
    private void executeSync(String cmd) throws ProcessCompletionException {
        try {
            log.trace("running create vhost cmd [{}}", cmd);
            RUNTIME.exec(cmd)
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
