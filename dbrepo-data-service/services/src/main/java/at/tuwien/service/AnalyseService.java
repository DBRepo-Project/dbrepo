package at.tuwien.service;

import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.TableNotFoundException;

public interface AnalyseService {
    TableStatisticDto analyseTable(Long databaseId, Long tableId) throws TableNotFoundException,
            NotAllowedException, RemoteUnavailableException;
}
