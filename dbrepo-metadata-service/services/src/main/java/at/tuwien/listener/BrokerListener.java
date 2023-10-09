package at.tuwien.listener;

import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import org.springframework.scheduling.annotation.Scheduled;

public interface BrokerListener {

    @Scheduled
    void updatePermissions() throws BrokerVirtualHostGrantException, BrokerRemoteException;
}
