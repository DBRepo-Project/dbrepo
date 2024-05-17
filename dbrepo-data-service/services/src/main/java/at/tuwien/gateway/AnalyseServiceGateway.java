package at.tuwien.gateway;

import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.TableNotFoundException;

public interface AnalyseServiceGateway {
    TableStatisticDto analyseTable(Long databaseId, Long tableId) throws RemoteUnavailableException, NotAllowedException, TableNotFoundException;
}
