package at.tuwien.listener;

import at.tuwien.exception.BrokerRemoteException;
import at.tuwien.exception.BrokerVirtualHostGrantException;
import org.springframework.scheduling.annotation.Scheduled;

public interface BrokerListener {

    /**
     * Update broker permissions.
     *
     * @throws BrokerVirtualHostGrantException
     * @throws BrokerRemoteException
     */
    @Scheduled
    void updatePermissions() throws BrokerVirtualHostGrantException, BrokerRemoteException;
}
