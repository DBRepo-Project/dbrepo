package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.DuckDbConfig;
import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.exception.AnalyseDataTypesException;
import at.ac.tuwien.ifs.dbrepo.core.mapper.MetadataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.DataMapper;
import at.ac.tuwien.ifs.dbrepo.mapper.DuckDbMapper;
import at.ac.tuwien.ifs.dbrepo.service.impl.AnalyseServiceDuckDbImpl;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AnalyseSingleObjectUnitTest {
    @ParameterizedTest
    @ValueSource(strings = {"*.csv", "data?.csv", "data[ab].csv", "data[.csv", "data[[]literal.csv",
            "folder/**/data.csv", "escape\\*.csv", "quote'*.csv", "日本語?.csv", "x'[1:0] || 'marker.csv"})
    void wildcardKeysFailBeforeOpeningOrSettingUpDuckDb(String key) throws Exception {
        S3Config s3 = mock(S3Config.class);
        when(s3.getS3Bucket()).thenReturn("uploads");
        DataMapper dataMapper = mock(DataMapper.class);
        DuckDbMapper sqlMapper = mock(DuckDbMapper.class);
        DuckDbConfig config = mock(DuckDbConfig.class);
        MetadataMapper metadata = mock(MetadataMapper.class);
        var service = spy(new AnalyseServiceDuckDbImpl(s3, dataMapper, config, sqlMapper, metadata));

        assertThrows(AnalyseDataTypesException.class, () -> service.determineS3CsvDataTypes(null, key));

        verify(service, never()).getDuckDbConnection();
        verify(service, never()).setup(any());
        verifyNoInteractions(dataMapper, sqlMapper, config, metadata);
    }
}
