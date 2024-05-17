package at.tuwien.service.impl;

import at.tuwien.api.database.table.TableStatisticDto;
import at.tuwien.exception.NotAllowedException;
import at.tuwien.exception.RemoteUnavailableException;
import at.tuwien.exception.TableNotFoundException;
import at.tuwien.gateway.AnalyseServiceGateway;
import at.tuwien.service.AnalyseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Log4j2
@Service
public class AnalyseServiceImpl implements AnalyseService {

    private final AnalyseServiceGateway analyseServiceGateway;

    @Autowired
    public AnalyseServiceImpl(AnalyseServiceGateway analyseServiceGateway) {
        this.analyseServiceGateway = analyseServiceGateway;
    }

    @Override
    public TableStatisticDto analyseTable(Long databaseId, Long tableId) throws TableNotFoundException,
            NotAllowedException, RemoteUnavailableException {
        return analyseServiceGateway.analyseTable(databaseId, tableId);
    }

}
