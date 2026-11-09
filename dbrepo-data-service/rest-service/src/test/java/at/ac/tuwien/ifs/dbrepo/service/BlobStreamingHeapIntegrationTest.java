package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.SeaweedFsContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import at.ac.tuwien.ifs.dbrepo.core.test.BaseTest;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.MariaDBContainer;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.*;
import java.net.URI;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class BlobStreamingHeapIntegrationTest {
    @TempDir Path work;

    @Test @Timeout(240)
    void largeOwnedBlobsRoundTripThroughAllMutationAndSerializationPathsWithSmallHeap() throws Exception {
        final long length=128L*1024*1024;
        final Path first=work.resolve("first"), second=work.resolve("second");
        for (var file:List.of(first,second)) try (var data=new RandomAccessFile(file.toFile(),"rw")) { data.setLength(length); data.seek(length-2); data.write(file.equals(first) ? new byte[]{-1,39} : new byte[]{92,1}); }
        final var cfg=new Properties();
        cfg.setProperty("length",Long.toString(length)); cfg.setProperty("bucket","blob-heap");
        try (var input=Files.newInputStream(first)) { cfg.setProperty("firstHash",DigestUtils.sha256Hex(input)); }
        try (var input=Files.newInputStream(second)) { cfg.setProperty("secondHash",DigestUtils.sha256Hex(input)); }
        try (var maria=new MariaDBContainer<>("mariadb:10.11").withDatabaseName("fixture")
                .withCommand("--max-allowed-packet=536870912").withUrlParam("sslMode","disable"); var seaweed=SeaweedFsContainerConfig.getContainer()) {
            maria.start(); seaweed.start();
            cfg.setProperty("host",maria.getHost()); cfg.setProperty("port",maria.getMappedPort(3306).toString());
            try (var connection=DriverManager.getConnection(maria.getJdbcUrl(),maria.getUsername(),maria.getPassword()); var statement=connection.createStatement()) {
                statement.execute("CREATE TABLE binary_records(id INT PRIMARY KEY,raw LONGBLOB,other LONGBLOB,label VARCHAR(32))");
            }
            cfg.setProperty("s3",SeaweedFsContainerConfig.getEndpoint(seaweed));
            try (var s3=S3Client.builder().endpointOverride(URI.create(cfg.getProperty("s3"))).region(Region.US_EAST_1).forcePathStyle(true)
                    .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("seaweedfsadmin","seaweedfsadmin"))).build()) {
                s3.createBucket(CreateBucketRequest.builder().bucket(cfg.getProperty("bucket")).build());
                final String owner=new BaseTest(){}.USER_1_ID.toString();
                for (String name:List.of("first","second","empty")) {
                    final String key=UploadMetadata.newKey(name);cfg.setProperty(name,key);
                    s3.putObject(PutObjectRequest.builder().bucket(cfg.getProperty("bucket")).key(key)
                            .metadata(Map.of(UploadMetadata.OWNER,owner)).build(),name.equals("empty") ? RequestBody.empty() : RequestBody.fromFile(name.equals("first") ? first : second));
                }
                final Path config=Files.createFile(work.resolve("connection.private.properties"),java.nio.file.attribute.PosixFilePermissions.asFileAttribute(java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
                try (var out=Files.newOutputStream(config)) { cfg.store(out,null); }
                final Path log=Path.of("target","blob-heap-probe.log").toAbsolutePath();
                final var builder=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx96m","-cp",
                        System.getProperty("surefire.test.class.path",System.getProperty("java.class.path")),BlobHeapProbe.class.getName(),config.toString())
                        .redirectErrorStream(true).redirectOutput(log.toFile());
                builder.environment().put("DBREPO_BLOB_TEST_USERNAME",maria.getUsername());
                builder.environment().put("DBREPO_BLOB_TEST_PASSWORD",maria.getPassword());
                final var process=builder.start();
                try {
                    assertTrue(process.waitFor(180,TimeUnit.SECONDS),()->read(log));
                    assertEquals(0,process.exitValue(),()->read(log));
                    assertTrue(read(log).contains("BLOB_HEAP_PASS"),()->read(log));
                    read(log).lines().filter(line->line.startsWith("BLOB_HEAP_PASS")).forEach(System.out::println);
                } finally { if (process.isAlive()) { process.destroyForcibly();process.waitFor(10,TimeUnit.SECONDS); } Files.deleteIfExists(config); }
            }
        }
    }
    private static String read(Path path) { try{return Files.readString(path);}catch(IOException error){return error.getMessage();} }
}
