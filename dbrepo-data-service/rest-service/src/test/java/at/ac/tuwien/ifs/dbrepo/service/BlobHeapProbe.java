package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.api.database.table.*;
import at.ac.tuwien.ifs.dbrepo.core.entity.cache.*;
import at.ac.tuwien.ifs.dbrepo.core.exception.*;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import at.ac.tuwien.ifs.dbrepo.endpoints.TableEndpoint;
import at.ac.tuwien.ifs.dbrepo.gateway.MetadataServiceGateway;
import at.ac.tuwien.ifs.dbrepo.mapper.*;
import at.ac.tuwien.ifs.dbrepo.service.impl.*;
import at.ac.tuwien.ifs.dbrepo.validation.EndpointValidator;
import com.fasterxml.jackson.core.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.mapstruct.factory.Mappers;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.*;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.time.Duration;
import java.util.*;

import static at.ac.tuwien.ifs.dbrepo.core.test.utils.EndpointUtils.jwtPrincipal;

/** Exercises actual endpoint permission checks, S3, JDBC and result serialization in a small heap. */
public final class BlobHeapProbe extends BaseTest {
    public static void main(String[] args) throws Exception { new BlobHeapProbe().run(Path.of(args[0])); }

    private void run(Path configFile) throws Exception {
        final var cfg = new Properties();
        try (var input = Files.newInputStream(configFile)) { cfg.load(input); }
        CONTAINER_1_CACHE.setHost(cfg.getProperty("host"));
        CONTAINER_1_CACHE.setPort(Integer.parseInt(cfg.getProperty("port")));
        CONTAINER_1_CACHE.setUsername(System.getenv("DBREPO_BLOB_TEST_USERNAME"));
        CONTAINER_1_CACHE.setPassword(System.getenv("DBREPO_BLOB_TEST_PASSWORD"));
        DATABASE_3_CACHE.setInternalName("fixture");
        TABLE_8_CACHE.setInternalName("binary_records");
        TABLE_8_CACHE.setColumns(List.of(
                Column.builder().internalName("id").columnType(ColumnType.INT).build(),
                Column.builder().internalName("raw").columnType(ColumnType.LONGBLOB).build(),
                Column.builder().internalName("other").columnType(ColumnType.LONGBLOB).build(),
                Column.builder().internalName("label").columnType(ColumnType.VARCHAR).build()));
        DATABASE_3_CACHE.setTables(List.of(TABLE_8_CACHE));
        DATABASE_3_CACHE.setAccesses(List.of(
                DatabaseAccess.builder().username(USER_1_USERNAME).type(AccessType.WRITE_ALL).build(),
                DatabaseAccess.builder().username(USER_3_USERNAME).type(AccessType.WRITE_ALL).build()));
        final var caller = jwtPrincipal(USER_1_PRINCIPAL, USER_1_ID);
        final var foreign = jwtPrincipal(USER_3_PRINCIPAL, USER_3_ID);
        final long length = Long.parseLong(cfg.getProperty("length"));
        final var mapper = Mappers.getMapper(MariaDbMapper.class);
        final var gateway = (MetadataServiceGateway) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{MetadataServiceGateway.class}, (proxy, method, values) -> {
                    if (method.getName().equals("getDatabaseById") && DATABASE_3_ID.equals(values[0])) return DATABASE_3_CACHE;
                    throw new UnsupportedOperationException(method.getName());
                });
        final String bucket = cfg.getProperty("bucket");
        final var s3Config = new S3Config() { @Override public String getS3Bucket() { return bucket; } };
        try (var s3 = S3Client.builder().endpointOverride(URI.create(cfg.getProperty("s3")))
                .region(Region.US_EAST_1).forcePathStyle(true)
                .httpClientBuilder(ApacheHttpClient.builder().maxConnections(2)
                        .connectionAcquisitionTimeout(Duration.ofSeconds(2)))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("seaweedfsadmin", "seaweedfsadmin"))).build()) {
            final StorageService storage = new StorageServiceS3Impl(s3Config, s3);
            final var reads = new DataServiceSparkImpl(null, null);
            final var service = new TableServiceMariaDbImpl(Mappers.getMapper(DataMapper.class), mapper, null, storage, null);
            final var endpoint = new TableEndpoint(reads, service, mapper, null, null,
                    new EndpointValidator(), gateway, storage, null);
            final String first = cfg.getProperty("first"), second = cfg.getProperty("second");
            var data = new LinkedHashMap<String,Object>();
            data.put("id", 1); data.put("raw", first); data.put("other", second); data.put("label", "tail,ä");
            try {
                endpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, TupleDto.builder().data(data).build(), foreign);
                throw new AssertionError("Foreign caller wrote owned uploads");
            } catch (StorageNotFoundException expected) { }
            check(endpoint.insertRawTuple(DATABASE_3_ID, TABLE_8_ID, TupleDto.builder().data(data).build(), caller).getStatusCode().value()==201);
            check(data.get("raw").equals(first) && data.get("other").equals(second));
            verifyRow(reads, length, cfg.getProperty("firstHash"), cfg.getProperty("secondHash"));
            final var replacement = new LinkedHashMap<String,Object>();
            replacement.put("raw", second); replacement.put("other", null);
            check(endpoint.updateRawTuple(DATABASE_3_ID, TABLE_8_ID,
                    TupleUpdateDto.builder().data(replacement).keys(Map.of("raw", first)).build(), caller).getStatusCode().value()==202);
            verifyRow(reads, length, cfg.getProperty("secondHash"), null);
            final Path output = configFile.resolveSibling("result");
            try {
                try (var file = Files.newOutputStream(output); var result = reads.query(DATABASE_3_CACHE, "SELECT id,raw,other,label FROM binary_records")) { result.writeJson(file); }
                try (var parser = new JsonFactory().createParser(output.toFile())) {
                    check(parser.nextToken()==JsonToken.START_ARRAY); check(parser.nextToken()==JsonToken.START_OBJECT);
                    check(parser.nextToken()==JsonToken.FIELD_NAME && parser.currentName().equals("id"));
                    check(parser.nextToken()==JsonToken.VALUE_NUMBER_INT && parser.getIntValue()==1);
                    check(parser.nextToken()==JsonToken.FIELD_NAME && parser.currentName().equals("raw"));
                    check(parser.nextToken()==JsonToken.VALUE_STRING);
                    final var digest=MessageDigest.getInstance("SHA-256");
                    try (var sink=new DigestOutputStream(OutputStream.nullOutputStream(),digest)) { check(parser.readBinaryValue(sink)==length); }
                    check(HexFormat.of().formatHex(digest.digest()).equals(cfg.getProperty("secondHash")));
                    check(parser.nextToken()==JsonToken.FIELD_NAME && parser.currentName().equals("other")); check(parser.nextToken()==JsonToken.VALUE_NULL);
                    check(parser.nextToken()==JsonToken.FIELD_NAME && parser.currentName().equals("label")); check(parser.nextToken()==JsonToken.VALUE_STRING && parser.getText().equals("tail,ä"));
                    check(parser.nextToken()==JsonToken.END_OBJECT); check(parser.nextToken()==JsonToken.END_ARRAY); check(parser.nextToken()==null);
                }
                try (var file = Files.newOutputStream(output); var result = reads.query(DATABASE_3_CACHE, "SELECT id,raw,other,label FROM binary_records")) { result.writeCsv(file); }
                try (var input = new BufferedInputStream(Files.newInputStream(output))) {
                    check(new String(input.readNBytes("id,raw,other,label".length()+System.lineSeparator().length()),java.nio.charset.StandardCharsets.UTF_8).equals("id,raw,other,label"+System.lineSeparator()));
                    check(input.read()=='1' && input.read()==',');
                    final var digest=MessageDigest.getInstance("SHA-256");
                    final var decoded=Base64.getDecoder().wrap(org.apache.commons.io.input.CloseShieldInputStream.wrap(input));
                    try (var sink=new DigestOutputStream(OutputStream.nullOutputStream(),digest)) { check(decoded.transferTo(sink)==length); }
                    check(HexFormat.of().formatHex(digest.digest()).equals(cfg.getProperty("secondHash")));
                    check(new String(input.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8).equals(",,\"tail,ä\""+System.lineSeparator()));
                }
            } finally { Files.deleteIfExists(output); }
            // An interrupted storage response must be closed, rolled back and reported as unavailable.
            final var closed=new java.util.concurrent.atomic.AtomicBoolean();
            final var broken=(StorageService)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{StorageService.class},(proxy,method,values)-> {
                if (!method.getName().equals("getObject")) throw new UnsupportedOperationException(method.getName());
                return new InputStream() {
                    private int remaining=65536;
                    @Override public int read() throws IOException { if (remaining-- > 0) return 0; throw new IOException("fixture interrupted S3 response"); }
                    @Override public int read(byte[] bytes,int offset,int length) throws IOException {
                        if (remaining<=0) throw new IOException("fixture interrupted S3 response");
                        int count=Math.min(remaining,length);Arrays.fill(bytes,offset,offset+count,(byte)0);remaining-=count;return count;
                    }
                    @Override public void close() { closed.set(true); }
                };
            });
            final var failingService=new TableServiceMariaDbImpl(Mappers.getMapper(DataMapper.class),mapper,null,broken,null);
            final var failingEndpoint=new TableEndpoint(reads,failingService,mapper,null,null,new EndpointValidator(),gateway,storage,null);
            try { failingEndpoint.insertRawTuple(DATABASE_3_ID,TABLE_8_ID,TupleDto.builder().data(Map.of("id",3,"raw",first)).build(),caller); throw new AssertionError("Interrupted BLOB accepted"); }
            catch (StorageUnavailableException expected) { check(closed.get()); }
            verifyRow(reads,length,cfg.getProperty("secondHash"),null);
            // Binding a later missing object must release an earlier unread S3 response.
            for (int attempt=0;attempt<4;attempt++) {
                var invalid=new LinkedHashMap<String,Object>(); invalid.put("id",2); invalid.put("raw",first); invalid.put("other","missing");
                try { service.createTuple(DATABASE_3_CACHE,TABLE_8_CACHE,TupleDto.builder().data(invalid).build()); throw new AssertionError("Missing BLOB accepted"); }
                catch (StorageNotFoundException expected) { }
            }
            // SQL failure must preserve the original row and release both S3 responses.
            for (int attempt=0;attempt<3;attempt++) {
                try { endpoint.insertRawTuple(DATABASE_3_ID,TABLE_8_ID,TupleDto.builder().data(data).build(),caller); throw new AssertionError("Duplicate row accepted"); }
                catch (QueryMalformedException expected) { }
            }
            check(endpoint.deleteRawTuple(DATABASE_3_ID, TABLE_8_ID,
                    TupleDeleteDto.builder().keys(Map.of("raw", second)).build(), caller).getStatusCode().value()==202);
            try (var result=reads.query(DATABASE_3_CACHE,"SELECT COUNT(*) AS total FROM binary_records")) {
                final var outputCount=new ByteArrayOutputStream(); result.writeJson(outputCount); check(outputCount.toString().contains("\"total\":0"));
            }
            // NULL and empty binary values stay distinct in JSON.
            var empty=new LinkedHashMap<String,Object>(); empty.put("id",2); empty.put("raw",cfg.getProperty("empty")); empty.put("other",null);
            endpoint.insertRawTuple(DATABASE_3_ID,TABLE_8_ID,TupleDto.builder().data(empty).build(),caller);
            try (var result=reads.query(DATABASE_3_CACHE,"SELECT raw,other FROM binary_records")) {
                final var bytes=new ByteArrayOutputStream(); result.writeJson(bytes); check(bytes.toString().equals("[{\"raw\":\"\",\"other\":null}]"));
            }
            final var nullKey=new HashMap<String,Object>(); nullKey.put("other",null);
            endpoint.updateRawTuple(DATABASE_3_ID,TABLE_8_ID,TupleUpdateDto.builder().data(Map.of("label","updated")).keys(nullKey).build(),caller);
            try (var result=reads.query(DATABASE_3_CACHE,"SELECT label FROM binary_records")) {
                final var bytes=new ByteArrayOutputStream();result.writeJson(bytes);check(bytes.toString().equals("[{\"label\":\"updated\"}]"));
            }
            endpoint.deleteRawTuple(DATABASE_3_ID,TABLE_8_ID,TupleDeleteDto.builder().keys(nullKey).build(),caller);
            endpoint.insertRawTuple(DATABASE_3_ID,TABLE_8_ID,TupleDto.builder().data(empty).build(),caller);
            endpoint.deleteRawTuple(DATABASE_3_ID,TABLE_8_ID,TupleDeleteDto.builder().keys(Map.of("raw",cfg.getProperty("empty"))).build(),caller);
            try (var result=reads.query(DATABASE_3_CACHE,"SELECT COUNT(*) AS total FROM binary_records")) {
                final var bytes=new ByteArrayOutputStream();result.writeJson(bytes);check(bytes.toString().equals("[{\"total\":0}]"));
            }
            System.out.printf("BLOB_HEAP_PASS bytes=%d heapMax=%d insert=201 update=202 delete=202 json=verified csv=verified cleanup=passed%n",length,Runtime.getRuntime().maxMemory());
        }
    }

    private void verifyRow(DataService reads, long length, String hash, String otherHash) throws Exception {
        try (var result=reads.query(DATABASE_3_CACHE,"SELECT OCTET_LENGTH(raw) AS size,SHA2(raw,256) AS digest,SHA2(other,256) AS other_digest FROM binary_records WHERE id=1")) {
            final var bytes=new ByteArrayOutputStream(); result.writeJson(bytes);
            final var row=new com.fasterxml.jackson.databind.ObjectMapper().readTree(bytes.toByteArray()).get(0);
            check(row.get("size").asLong()==length && row.get("digest").asText().equals(hash));
            check(otherHash==null ? row.get("other_digest").isNull() : row.get("other_digest").asText().equals(otherHash));
        }
    }

    private static void check(boolean condition) { if (!condition) throw new AssertionError("BLOB fixture assertion failed"); }
}
