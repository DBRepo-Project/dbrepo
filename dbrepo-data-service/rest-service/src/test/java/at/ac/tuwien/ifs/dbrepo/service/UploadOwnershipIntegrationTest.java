package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.*;
import at.ac.tuwien.ifs.dbrepo.core.api.database.query.ImportDto;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.TupleDto;
import at.ac.tuwien.ifs.dbrepo.core.exception.StorageNotFoundException;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.AnalyseEndpoint;
import at.ac.tuwien.ifs.dbrepo.endpoints.TableEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.utils.MariaDbUtil;
import at.ac.tuwien.ifs.dbrepo.utils.S3Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.zip.GZIPOutputStream;

import static at.ac.tuwien.ifs.dbrepo.core.test.utils.EndpointUtils.jwtPrincipal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UploadOwnershipIntegrationTest extends BaseTest {
    @Autowired private MockMvc mvc;
    @Autowired private StorageService storage;
    @Autowired private S3Client s3;
    @Autowired private S3Config config;
    @Autowired private TableEndpoint tables;
    @Autowired private AnalyseEndpoint analysis;
    @MockitoBean private org.springframework.security.oauth2.jwt.JwtDecoder decoder;
    @MockitoBean private MetadataService metadata;
    @MockitoBean private MetadataServiceGateway gateway;
    @Container private static final MariaDBContainer<?> maria = MariaDbContainerConfig.getContainer();
    @Container private static final GenericContainer<?> seaweed = SeaweedFsContainerConfig.getContainer();

    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("dbrepo.spark.hadoop.fs.s3a.endpoint", () -> SeaweedFsContainerConfig.getEndpoint(seaweed));
    }

    @BeforeEach void setup() throws Exception {
        for (var principal : List.of(jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID), jwtPrincipal(USER_3_PRINCIPAL, USER_3_ID))) {
            var original = (org.springframework.security.oauth2.jwt.Jwt) ((Authentication) principal).getPrincipal();
            var jwt = org.springframework.security.oauth2.jwt.Jwt.withTokenValue(original.getSubject()).header("alg", "RS256")
                    .subject(original.getSubject()).claim("preferred_username", original.getClaimAsString("preferred_username"))
                    .claim("realm_access", Map.of("roles", List.of("upload-file"))).build();
            when(decoder.decode(original.getSubject())).thenReturn(jwt);
        }
        S3Util.cleanBucket(s3, config);
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_1_INTERNAL_NAME);
        MariaDbUtil.dropDatabase(CONTAINER_1_CACHE, DATABASE_3_INTERNAL_NAME);
        MariaDbUtil.createInitDatabase(DATABASE_1_CACHE);
        MariaDbUtil.createInitDatabase(DATABASE_3_CACHE);
        DATABASE_1_CACHE.setAccesses(List.of(
                at.ac.tuwien.ifs.dbrepo.core.entity.cache.DatabaseAccess.builder().username(USER_1_USERNAME)
                        .type(at.ac.tuwien.ifs.dbrepo.core.entity.cache.AccessType.WRITE_ALL).build(),
                DATABASE_1_USER_3_WRITE_ALL_ACCESS_CACHE));
        when(gateway.getDatabaseById(DATABASE_1_ID)).thenReturn(DATABASE_1_CACHE);
        DATABASE_1_CACHE.setTables(List.of(TABLE_1_CACHE));
        when(gateway.getDatabaseById(DATABASE_3_ID)).thenReturn(DATABASE_3_CACHE);
        DATABASE_3_CACHE.setTables(List.of(TABLE_8_CACHE));
        var dataTypes = new ArrayList<>(IMAGE_1_CACHE.getDataTypes());
        dataTypes.add(at.ac.tuwien.ifs.dbrepo.core.entity.cache.DataType.builder().value("double").build());
        IMAGE_1_CACHE.setDataTypes(dataTypes);
        when(metadata.getImage(IMAGE_1_ID)).thenReturn(IMAGE_1_CACHE);
    }

    private String upload(String filename, byte[] bytes, Authentication caller) throws Exception {
        var context = org.springframework.security.core.context.SecurityContextHolder.getContext();
        try {
            return mvc.perform(multipart("/api/v1/upload")
                            .file(new MockMultipartFile("file", filename, "application/octet-stream", bytes))
                            .header("Authorization", "Bearer " + caller.getName()))
                    .andExpect(status().isCreated()).andReturn().getResponse().getHeader("X-S3-Key");
        } finally {
            org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        }
    }

    @Test
    @WithMockUser(authorities = {"insert-table-data"})
    void concurrentSameFilenameUploadsRemainSeparateAndEachWriterImportsOwnBytes() throws Exception {
        var alice = (Authentication) jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID);
        var bob = (Authentication) jwtPrincipal(USER_3_PRINCIPAL, USER_3_ID);
        byte[] first = "id,date,location,mintemp,rainfall\n4,2023-10-03,Albury,15,0.2\n".getBytes(StandardCharsets.UTF_8);
        byte[] second = "id,date,location,mintemp,rainfall\n5,2023-10-04,Vienna,16,0.3\n".getBytes(StandardCharsets.UTF_8);
        String firstKey;
        String secondKey;
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> upload("same.csv", first, alice));
            var b = pool.submit(() -> upload("same.csv", second, bob));
            firstKey = a.get(); secondKey = b.get();
        }
        assertNotEquals(firstKey, secondKey);
        assertTrue(UploadMetadata.isUploadKey(firstKey));
        assertArrayEquals(first, storage.getBytes(firstKey));
        assertArrayEquals(second, storage.getBytes(secondKey));
        var stored = s3.headObject(HeadObjectRequest.builder().bucket(config.getS3Bucket()).key(firstKey).build());
        assertEquals(USER_1_ID.toString(), stored.metadata().get(UploadMetadata.OWNER));
        assertEquals("same.csv", new String(Base64.getDecoder().decode(stored.metadata().get(UploadMetadata.FILENAME)), StandardCharsets.UTF_8));
        assertThrows(StorageNotFoundException.class, () -> tables.importDataset(DATABASE_1_ID, TABLE_1_ID,
                csv(firstKey), bob, "caller-token"));
        verify(gateway).getDatabaseById(DATABASE_1_ID);
        verifyNoMoreInteractions(gateway);
        tables.importDataset(DATABASE_1_ID, TABLE_1_ID, csv(firstKey), alice, "caller-token");
        tables.importDataset(DATABASE_1_ID, TABLE_1_ID, csv(secondKey), bob, "caller-token");
        var rows = MariaDbUtil.selectQuery(DATABASE_1_CACHE, "SELECT location FROM weather_aus WHERE id IN (4,5) ORDER BY id", Set.of("location"));
        assertEquals(List.of("Albury", "Vienna"), rows.stream().map(row -> row.get("location")).toList());
        assertThrows(StorageNotFoundException.class, () -> storage.requireUploadOwner(firstKey, USER_1_ID.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"data", "data.txt", "data.csv", "data.gz", "data.tsv.gz"})
    @WithMockUser(authorities = {"insert-table-data", "analyse-datatypes"})
    void allocatedFilenameWorksWithDuckDbAndSpark(String filename) throws Exception {
        var caller = (Authentication) jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID);
        String delimiter = filename.contains(".tsv") ? "\t" : ",";
        byte[] bytes = ("id,date,location,mintemp,rainfall\n4,2023-10-03,Vienna,15,0.2\n".replace(",", delimiter))
                .getBytes(StandardCharsets.UTF_8);
        if (filename.endsWith(".gz")) {
            var output = new ByteArrayOutputStream();
            try (var gzip = new GZIPOutputStream(output)) { gzip.write(bytes); }
            bytes = output.toByteArray();
        }
        String key = upload(filename, bytes, caller);
        var schema = analysis.analyseDatatypes(IMAGE_1_ID, key, caller).getBody();
        assertNotNull(schema);
        assertEquals(5, schema.getColumns().size());
        assertThrows(StorageNotFoundException.class, () -> analysis.analyseDatatypes(IMAGE_1_ID, key,
                jwtPrincipal(USER_3_PRINCIPAL, USER_3_ID)));
        var request = csv(key);
        request.setSeparator(delimiter.charAt(0));
        tables.importDataset(DATABASE_1_ID, TABLE_1_ID, request, caller, "caller-token");
        assertEquals("Vienna", MariaDbUtil.selectQuery(DATABASE_1_CACHE,
                "SELECT location FROM weather_aus WHERE id=4", Set.of("location")).getFirst().get("location"));
    }

    @Test
    @WithMockUser(authorities = {"insert-table-data"})
    void ownBinaryUploadIsWrittenAsExactBlobBytes() throws Exception {
        var caller = (Authentication) jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID);
        byte[] bytes = new byte[]{0, 1, 2, -1, 7, 0};
        String key = upload("image.png", bytes, caller);
        var data = new HashMap<String, Object>();
        data.put("value", "24.3"); data.put("raw", key);
        tables.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, TupleDto.builder().data(data).build(), caller);
        var rows = MariaDbUtil.selectQueryByteArr(DATABASE_3_CACHE, "SELECT raw FROM mfcc WHERE raw IS NOT NULL", Set.of("raw"));
        assertArrayEquals(bytes, rows.getFirst().get("raw"));
        assertEquals(key, data.get("raw"));
    }

    private ImportDto csv(String key) {
        return ImportDto.builder().location(key).header(true).separator(',').build();
    }
}
