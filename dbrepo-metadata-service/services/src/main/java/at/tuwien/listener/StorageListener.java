package at.tuwien.listener;

import at.tuwien.exception.FileStorageException;
import org.springframework.scheduling.annotation.Scheduled;

public interface StorageListener {

    /**
     * Deletes old files from the buckets used by the system in regular intervals.
     *
     * @throws FileStorageException The object failed to be loaded from the Storage Service.
     */
    @Scheduled
    void deleteStaleFiles() throws FileStorageException;
}
