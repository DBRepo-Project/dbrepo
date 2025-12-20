package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.core.api.analyse.ColumnAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.analyse.SchemaAnalysisResultDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.QueryDto;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Image;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;

import java.util.Map;

public interface AnalyseService {

    /**
     * Determines the data types for a given dataset S3 key and maps it to the given database engine image.
     *
     * @param image The database engine image.
     * @param key   The S3 key.
     * @return The data types.
     * @throws AnalyseDataTypesException    The result set is empty, cannot determine data types.
     * @throws DatabaseUnavailableException The target database is not available.
     * @throws StorageNotFoundException     The dataset was not found with the given S3 key.
     * @throws ColumnNotFoundException      The column was not found in the dataset with the given S3 key.
     * @throws ImageInvalidException        The provided image does not contain the analysed data type.
     */
    SchemaAnalysisResultDto determineS3CsvDataTypes(Image image, String key) throws AnalyseDataTypesException,
            DatabaseUnavailableException, StorageNotFoundException, ColumnNotFoundException, ImageInvalidException;

    /**
     * Determines the data types for a given subset and maps it to the given database engine image.
     *
     * @param database The database.
     * @param subset   The subset.
     * @return The data types.
     * @throws AnalyseDataTypesException    The result set is empty, cannot determine data types.
     * @throws ColumnNotFoundException      The column was not found in the dataset with the given S3 key.
     * @throws DatabaseUnavailableException The target database is not available.
     */
    Map<String, ColumnAnalysisResultDto> determineDataTypes(Database database, QueryDto subset)
            throws AnalyseDataTypesException, ColumnNotFoundException, DatabaseUnavailableException;

    /**
     * Determines the data types for a given subset statement and maps it to the given database engine image.
     *
     * @param database  The database.
     * @param statement The statement.
     * @return The data types.
     * @throws ColumnNotFoundException      The column was not found in the dataset with the given S3 key.
     * @throws DatabaseUnavailableException The target database is not available.
     */
    Map<String, ColumnAnalysisResultDto> determineDataTypes(Database database, String statement)
            throws ColumnNotFoundException, DatabaseUnavailableException;
}
