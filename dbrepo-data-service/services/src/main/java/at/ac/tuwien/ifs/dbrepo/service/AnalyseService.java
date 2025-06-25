package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.AnalyseDataTypesException;
import at.ac.tuwien.ifs.dbrepo.core.exception.DatabaseUnavailableException;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;

import java.sql.Connection;
import java.sql.SQLException;

public interface AnalyseService {
    void setup(Connection connection) throws SQLException;

    SchemaAnalysisResultDto determineDataTypes(String key) throws AnalyseDataTypesException, DatabaseUnavailableException, StorageNotFoundException;
}
