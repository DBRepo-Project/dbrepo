package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.Database;
import at.ac.tuwien.ifs.dbrepo.service.impl.DataServiceSparkImpl;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.classic.Dataset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class DataServiceSparkImplUnitTest {

    private static SparkSession sparkSession;

    @BeforeAll
    static void setUpSpark() {
        sparkSession = SparkSession.builder()
                .appName("DataServiceSparkImplUnitTest")
                .master("local[1]")
                .config("spark.ui.enabled", "false")
                .getOrCreate();
    }

    @AfterAll
    static void tearDownSpark() {
        if (sparkSession != null) {
            sparkSession.stop();
            sparkSession = null;
        }
    }

    @Test
    void getSubsetAsJson_withColumns_reordersDatasetColumns() throws Exception {
        final S3Config s3Config = mock(S3Config.class);
        final DataServiceSparkImpl service = spy(new DataServiceSparkImpl(s3Config, sparkSession));
        final Database database = mock(Database.class);
        final Dataset<Row> unorderedDataset = (Dataset<Row>) sparkSession.sql("select 2 as b, 1 as a, 3 as c");
        final String query = "select * from test_table";
        final List<String> requestedColumns = List.of("a", "c", "b");

        doReturn(unorderedDataset).when(service).getSubsetAsJson(database, query);

        final Dataset<Row> reorderedDataset = service.getSubsetAsJson(database, query, requestedColumns);

        assertArrayEquals(new String[]{"a", "c", "b"}, reorderedDataset.columns());
    }

    @Test
    void getSubsetAsJson_withColumns_addsMissingColumnsBeforeReordering() throws Exception {
        final S3Config s3Config = mock(S3Config.class);
        final DataServiceSparkImpl service = spy(new DataServiceSparkImpl(s3Config, sparkSession));
        final Database database = mock(Database.class);
        final Dataset<Row> incompleteDataset = (Dataset<Row>) sparkSession.sql("select 1 as a, 3 as c");
        final String query = "select * from test_table";
        final List<String> requestedColumns = List.of("a", "b", "c");

        doReturn(incompleteDataset).when(service).getSubsetAsJson(database, query);

        final Dataset<Row> reorderedDataset = service.getSubsetAsJson(database, query, requestedColumns);

        assertArrayEquals(new String[]{"a", "b", "c"}, reorderedDataset.columns());
    }
}
