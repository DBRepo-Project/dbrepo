package at.tuwien.config;

import at.tuwien.BaseUnitTest;
import at.tuwien.service.SyncService;
import lombok.extern.log4j.Log4j2;
import org.junit.Rule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.rules.Timeout;
import org.opensearch.client.RequestOptions;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.client.indices.GetIndexRequest;
import org.opensearch.client.indices.GetMappingsRequest;
import org.opensearch.client.indices.GetMappingsResponse;
import org.opensearch.testcontainers.OpensearchContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@Log4j2
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ExtendWith(SpringExtension.class)
public class IndexConfigComponentTest extends BaseUnitTest {

    @Autowired
    private SyncService syncService;

    @Autowired
    private RestHighLevelClient opensearchClient;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(60);

    @Container
    private static final OpensearchContainer opensearchContainer = new OpensearchContainer(DockerImageName.parse("opensearchproject/opensearch:2.10.0"));

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry) {
        final int idx = opensearchContainer.getHttpHostAddress().lastIndexOf(':');
        registry.add("spring.opensearch.host", () -> "127.0.0.1");
        registry.add("spring.opensearch.port", () -> opensearchContainer.getHttpHostAddress().substring(idx + 1));
        registry.add("spring.opensearch.username", opensearchContainer::getUsername);
        registry.add("spring.opensearch.password", opensearchContainer::getPassword);
    }

    @BeforeEach
    public void beforeEach() {
        syncService.start();
    }

    @Test
    public void index_conceptExists_succeeds() throws IOException {

        /* test */
        assertTrue(opensearchClient.indices()
                .exists(new GetIndexRequest("concept"), RequestOptions.DEFAULT));
    }

    @Test
    public void index_databaseExists_succeeds() throws IOException {

        /* test */
        assertTrue(opensearchClient.indices()
                .exists(new GetIndexRequest("database"), RequestOptions.DEFAULT));
    }

    @Test
    public void index_identifierExists_succeeds() throws IOException {

        /* test */
        assertTrue(opensearchClient.indices()
                .exists(new GetIndexRequest("identifier"), RequestOptions.DEFAULT));
    }

    @Test
    public void index_columnExists_succeeds() throws IOException {

        /* test */
        assertTrue(opensearchClient.indices()
                .exists(new GetIndexRequest("column"), RequestOptions.DEFAULT));
    }

    @Test
    public void index_tableExists_succeeds() throws IOException {

        /* test */
        assertTrue(opensearchClient.indices()
                .exists(new GetIndexRequest("table"), RequestOptions.DEFAULT));
    }

    @Test
    public void index_unitExists_succeeds() throws IOException {

        /* test */
        assertTrue(opensearchClient.indices()
                .exists(new GetIndexRequest("unit"), RequestOptions.DEFAULT));
    }

    @Test
    public void index_userExists_succeeds() throws IOException {

        /* test */
        assertTrue(opensearchClient.indices()
                .exists(new GetIndexRequest("user"), RequestOptions.DEFAULT));
    }

    @Test
    public void index_viewExists_succeeds() throws IOException {

        /* test */
        assertTrue(opensearchClient.indices()
                .exists(new GetIndexRequest("view"), RequestOptions.DEFAULT));
    }

    @Test
    public void index_conceptMapping_succeeds() throws IOException {

        /* test */
        final GetMappingsResponse response = opensearchClient.indices()
                .getMapping(new GetMappingsRequest().indices("concept"), RequestOptions.DEFAULT);
        final Map<String, String> types = getTypes("concept", response);
        assertEquals("keyword", types.get("id"));
        assertEquals("keyword", types.get("uri"));
        assertEquals("keyword", types.get("name"));
        assertEquals("text", types.get("description"));
        assertEquals("date", types.get("created"));
        assertNull(types.get("columns"));
    }

    @Test
    public void index_unitMapping_succeeds() throws IOException {

        /* test */
        final GetMappingsResponse response = opensearchClient.indices()
                .getMapping(new GetMappingsRequest().indices("unit"), RequestOptions.DEFAULT);
        final Map<String, String> types = getTypes("unit", response);
        assertEquals("keyword", types.get("id"));
        assertEquals("keyword", types.get("uri"));
        assertEquals("keyword", types.get("name"));
        assertEquals("text", types.get("description"));
        assertEquals("date", types.get("created"));
        assertNull(types.get("columns"));
    }

    @Test
    public void index_databaseMapping_succeeds() throws IOException {

        /* test */
        final GetMappingsResponse response = opensearchClient.indices()
                .getMapping(new GetMappingsRequest().indices("database"), RequestOptions.DEFAULT);
        final Map<String, String> types = getTypes("database", response);
        assertEquals("keyword", types.get("id"));
        assertEquals("keyword", types.get("name"));
        assertEquals("keyword", types.get("exchange_name"));
        assertEquals("keyword", types.get("internal_name"));
        assertNull(types.get("tables"));
        assertNull(types.get("views"));
        assertEquals("boolean", types.get("is_public"));
        assertNull(types.get("image"));
        assertEquals("nested", types.get("container"));
        assertNull(types.get("accesses"));
        assertEquals("nested", types.get("creator"));
        assertEquals("nested", types.get("owner"));
        assertEquals("date", types.get("created"));
    }

    @Test
    public void index_identifierMapping_succeeds() throws IOException {

        /* test */
        final GetMappingsResponse response = opensearchClient.indices()
                .getMapping(new GetMappingsRequest().indices("identifier"), RequestOptions.DEFAULT);
        final Map<String, String> types = getTypes("identifier", response);
        assertEquals("keyword", types.get("id"));
        assertEquals("keyword", types.get("database_id"));
        assertEquals("keyword", types.get("query_id"));
        assertEquals("keyword", types.get("view_id"));
        assertEquals("keyword", types.get("type"));
        assertEquals("nested", types.get("titles"));
        assertEquals("nested", types.get("descriptions"));
        assertEquals("nested", types.get("funders"));
        assertEquals("text", types.get("query"));
        assertEquals("text", types.get("query_normalized"));
        assertEquals("nested", types.get("related_identifiers"));
        assertNull(types.get("database"));
        assertEquals("text", types.get("query_hash"));
        assertEquals("date", types.get("execution"));
        assertEquals("text", types.get("result_hash"));
        assertEquals("long", types.get("result_number"));
        assertEquals("keyword", types.get("visibility"));
        assertEquals("keyword", types.get("doi"));
        assertEquals("text", types.get("publisher"));
        assertEquals("nested", types.get("creator"));
        assertEquals("integer", types.get("publication_day"));
        assertEquals("integer", types.get("publication_month"));
        assertEquals("integer", types.get("publication_year"));
        assertEquals("keyword", types.get("language"));
        assertEquals("nested", types.get("licenses"));
        assertEquals("nested", types.get("creators"));
        assertEquals("date", types.get("created"));
    }

    @Test
    public void index_viewMapping_succeeds() throws IOException {

        /* test */
        final GetMappingsResponse response = opensearchClient.indices()
                .getMapping(new GetMappingsRequest().indices("view"), RequestOptions.DEFAULT);
        final Map<String, String> types = getTypes("view", response);
        assertEquals("keyword", types.get("id"));
        assertEquals("keyword", types.get("database_id"));
        assertNull(types.get("database"));
        assertEquals("keyword", types.get("name"));
        assertNull(types.get("identifier"));
        assertEquals("keyword", types.get("internal_name"));
        assertEquals("boolean", types.get("is_public"));
        assertEquals("boolean", types.get("initial_view"));
        assertEquals("text", types.get("query"));
        assertEquals("keyword", types.get("query_hash"));
        assertEquals("date", types.get("created"));
        assertNull(types.get("created_by"));
        assertEquals("nested", types.get("creator"));
        assertNull(types.get("columns"));
        assertNull(types.get("last_modified"));
    }

    @Test
    public void index_tableMapping_succeeds() throws IOException {

        /* test */
        final GetMappingsResponse response = opensearchClient.indices()
                .getMapping(new GetMappingsRequest().indices("table"), RequestOptions.DEFAULT);
        final Map<String, String> types = getTypes("table", response);
        assertEquals("keyword", types.get("id"));
        assertNull(types.get("database"));
        assertEquals("keyword", types.get("name"));
        assertEquals("keyword", types.get("internal_name"));
        assertEquals("boolean", types.get("is_versioned"));
        assertNull(types.get("created_by"));
        assertEquals("nested", types.get("creator"));
        assertEquals("nested", types.get("owner"));
        assertEquals("keyword", types.get("queue_name"));
        assertEquals("keyword", types.get("routing_key"));
        assertEquals("text", types.get("description"));
        assertEquals("boolean", types.get("is_public"));
        assertEquals("date", types.get("created"));
        assertNull(types.get("columns"));
        assertEquals("nested", types.get("constraints"));
    }

    @Test
    public void index_userMapping_succeeds() throws IOException {

        /* test */
        final GetMappingsResponse response = opensearchClient.indices()
                .getMapping(new GetMappingsRequest().indices("user"), RequestOptions.DEFAULT);
        final Map<String, String> types = getTypes("user", response);
        assertEquals("keyword", types.get("id"));
        assertEquals("keyword", types.get("username"));
        assertEquals("keyword", types.get("name"));
        assertEquals("keyword", types.get("firstname"));
        assertEquals("keyword", types.get("lastname"));
        assertEquals("nested", types.get("attributes"));
        assertNull(types.get("email"));
    }

    private Map<String, String> getTypes(String indexName, GetMappingsResponse data) {
        final Map<String, Map<String, String>> properties = (Map<String, Map<String, String>>) data.mappings().get(indexName).getSourceAsMap().get("properties");
        final Map<String, String> types = new LinkedHashMap<>();
        properties.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals("_class"))
                .forEach(entry -> types.put(entry.getKey(), entry.getValue().get("type")));
        return types;
    }
}
