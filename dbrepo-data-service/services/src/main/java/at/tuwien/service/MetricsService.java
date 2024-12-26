package at.tuwien.service;

public interface MetricsService {

    void countTableGetData(Long databaseId, Long tableId);

    void countSubsetGetData(Long databaseId, Long subsetId);

    void countViewGetData(Long databaseId, Long viewId);
}
