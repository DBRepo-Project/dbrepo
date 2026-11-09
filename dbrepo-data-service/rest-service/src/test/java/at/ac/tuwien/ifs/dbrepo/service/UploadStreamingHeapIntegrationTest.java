package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.SeaweedFsContainerConfig;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.RandomAccessFile;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class UploadStreamingHeapIntegrationTest {
    @TempDir Path work;
    private final ObjectMapper json = new ObjectMapper();

    @Test
    @Timeout(180)
    void uploadLargerThanHeapPreservesContentAndAllowsConcurrentSmallUpload() throws Exception {
        final long length = 256L * 1024 * 1024;
        final Path payload = work.resolve("large.csv");
        try (var file = new RandomAccessFile(payload.toFile(), "rw")) { file.setLength(length); }
        final Path small = Files.writeString(work.resolve("small.csv"), "id,value\n1,small\n");
        final Path stage = Files.createDirectory(work.resolve("stage"));
        final Path server = Files.createDirectory(work.resolve("server"));
        final Path log = Path.of("target", "upload-heap-server.log").toAbsolutePath();
        try (var seaweed = SeaweedFsContainerConfig.getContainer()) {
            seaweed.start();
            final String s3Endpoint = SeaweedFsContainerConfig.getEndpoint(seaweed);
            try (var s3 = S3Client.builder().endpointOverride(URI.create(s3Endpoint)).region(Region.US_EAST_1)
                    .forcePathStyle(true).credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("seaweedfsadmin", "seaweedfsadmin"))).build()) {
                final String bucket = "heap-upload-test";
                s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
                s3.putObject(PutObjectRequest.builder().bucket(bucket).key("warmup").build(), RequestBody.fromString("warmup"));
                final var process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(),
                        "-Xmx96m", "-Djava.io.tmpdir=" + stage, "-cp",
                        System.getProperty("surefire.test.class.path", System.getProperty("java.class.path")),
                        UploadHeapServer.class.getName(), s3Endpoint, bucket, server.toString())
                        .redirectErrorStream(true).redirectOutput(log.toFile()).start();
                try (var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build()) {
                    String endpoint = null;
                    final long startupDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                    while (System.nanoTime() < startupDeadline) {
                        assertTrue(process.isAlive(), () -> readLog(log));
                        final var port = Files.readAllLines(log).stream().filter(line -> line.startsWith("UPLOAD_PORT=")).findFirst();
                        if (port.isPresent()) { endpoint = "http://127.0.0.1:" + port.get().split("=", 2)[1]; break; }
                        Thread.sleep(50);
                    }
                    assertNotNull(endpoint, () -> readLog(log));
                    final URI state = URI.create(endpoint + "/state");
                    final var large = http.sendAsync(upload(endpoint, payload), HttpResponse.BodyHandlers.ofString());
                    final long readyDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
                    boolean ready = false;
                    while (System.nanoTime() < readyDeadline) {
                        final var value = json.readTree(http.send(HttpRequest.newBuilder(state).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
                        if (value.get("largeReady").asBoolean()) { ready = true; break; }
                        Thread.sleep(25);
                    }
                    assertTrue(ready, () -> readLog(log));
                    final var smallResponse = http.send(upload(endpoint, small), HttpResponse.BodyHandlers.ofString());
                    assertEquals(201, smallResponse.statusCode(), smallResponse.body());
                    assertFalse(large.isDone(), "Small upload must finish while the large request is still active");
                    http.send(HttpRequest.newBuilder(state).POST(HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.discarding());
                    final HttpResponse<String> response;
                    try { response = large.get(90, TimeUnit.SECONDS); }
                    catch (Exception failure) { throw new AssertionError(readLog(log), failure); }
                    assertEquals(201, response.statusCode(), () -> response.body() + "\n" + readLog(log));
                    final String key = response.headers().firstValue("X-S3-Key").orElseThrow();
                    assertTrue(UploadMetadata.isUploadKey(key));
                    final var metadata = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
                    assertEquals(length, metadata.contentLength());
                    assertEquals(UploadHeapServer.OWNER, metadata.metadata().get(UploadMetadata.OWNER));
                    final String hash;
                    try (var input = Files.newInputStream(payload)) { hash = DigestUtils.sha256Hex(input); }
                    assertEquals(hash, metadata.metadata().get("sha256"));
                    try (var input = s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build())) {
                        assertEquals(hash, DigestUtils.sha256Hex(input));
                    }
                    final var measured = json.readTree(http.send(HttpRequest.newBuilder(state).GET().build(), HttpResponse.BodyHandlers.ofString()).body());
                    assertTrue(measured.get("heapMax").asLong() < length);
                    assertEquals(0, measured.get("active").asInt());
                    try (var files = Files.list(stage)) { assertEquals(0, files.count(), "Application staging files remain"); }
                    try (var files = Files.list(server.resolve("parts"))) { assertEquals(0, files.count(), "Servlet multipart files remain"); }
                    System.out.printf("UPLOAD_HEAP_EVIDENCE bytes=%d heapMax=%d sampledHeapPeak=%d concurrentSmall=201 tempFiles=0%n",
                            length, measured.get("heapMax").asLong(), measured.get("sampledHeapPeak").asLong());
                } finally {
                    process.destroy();
                    if (!process.waitFor(10, TimeUnit.SECONDS)) process.destroyForcibly().waitFor(10, TimeUnit.SECONDS);
                }
            }
        }
    }

    private static HttpRequest upload(String endpoint, Path payload) throws Exception {
        final String boundary = "dbrepo-synthetic-boundary";
        final byte[] prefix = ("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"data.csv\"\r\n"
                + "Content-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8);
        return HttpRequest.newBuilder(URI.create(endpoint + "/api/v1/upload")).timeout(Duration.ofSeconds(120))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.concat(HttpRequest.BodyPublishers.ofByteArray(prefix),
                        HttpRequest.BodyPublishers.ofFile(payload), HttpRequest.BodyPublishers.ofByteArray(
                                ("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8)))).build();
    }

    private static String readLog(Path log) {
        try { return Files.readString(log); } catch (Exception error) { return error.getClass().getSimpleName(); }
    }
}
