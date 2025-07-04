package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.container.image.ImageDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.sql.Connection;
import java.sql.SQLException;

public interface AnalyseService {
    void setup(Connection connection) throws SQLException;

    SchemaAnalysisResultDto determineDataTypes(ImageDto image, String key) throws AnalyseDataTypesException,
            DatabaseUnavailableException, StorageNotFoundException, ColumnNotFoundException, ImageInvalidException;
}
