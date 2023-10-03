package at.tuwien.service;

public interface SyncService {

    /**
     * Synchronizes the known entities in the metadata database into the search database.
     */
    void synchronize();
}
