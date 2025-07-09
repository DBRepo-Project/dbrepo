package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.DatabaseDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

public interface AnalyseService {
    void setupS3(Connection connection) throws SQLException;

    void setupMySql(Connection connection) throws SQLException;

    SchemaAnalysisResultDto determineDataTypes(ImageDto image, String key) throws AnalyseDataTypesException,
            DatabaseUnavailableException, StorageNotFoundException, ColumnNotFoundException, ImageInvalidException;

    Map<String, ColumnAnalysisResultDto> determineDataTypes(DatabaseDto database, QueryDto subset) throws AnalyseDataTypesException, ColumnNotFoundException, DatabaseUnavailableException;
}
