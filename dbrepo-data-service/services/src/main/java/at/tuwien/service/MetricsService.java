package at.tuwien.service;

import java.util.UUID;

public interface MetricsService {

    void countTableGetData(UUID databaseId, UUID tableId);

    void countSubsetGetData(UUID databaseId, UUID subsetId);

    void countViewGetData(UUID databaseId, UUID viewId);
}
