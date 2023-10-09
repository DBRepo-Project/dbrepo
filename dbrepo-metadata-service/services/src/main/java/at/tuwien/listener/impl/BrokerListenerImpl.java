package at.tuwien.listener.impl;

import at.tuwien.entities.user.User;
import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import at.tuwien.listener.BrokerListener;
import at.tuwien.repository.mdb.UserRepository;
import at.tuwien.service.MessageQueueService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Log4j2
@Component
public class BrokerListenerImpl implements BrokerListener {

    private final UserRepository userRepository;
    private final MessageQueueService messageQueueService;

    @Autowired
    public BrokerListenerImpl(UserRepository userRepository, MessageQueueService messageQueueService) {
        this.userRepository = userRepository;
        this.messageQueueService = messageQueueService;
    }

    @Override
    @Transactional(readOnly = true)
    @Scheduled(fixedRate = 60000)
    public void updatePermissions() throws BrokerVirtualHostGrantException, BrokerRemoteException {
        final List<User> users = userRepository.findAll();
        log.trace("updating permissions for {} users in the broker service", users.size());
        for (User user : users) {
            messageQueueService.setTopicExchangePermissions(user);
        }
    }

}
