package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.core.storage.UploadMetadata;
import at.ac.tuwien.ifs.dbrepo.service.impl.StorageServiceS3Impl;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.core.retry.RetryPolicy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.*;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UploadFileRetryTest {
    @TempDir Path temporary;

    @Test
    void s3RetrySendsTheWholeFileAgainWithTheSameOwnerAndConditionalKey() throws Exception {
        final String owner = "b3468012-3375-4d80-b6b0-58b72f96594f";
        final byte[] content = "id,value\n1,streamed\n".getBytes(StandardCharsets.UTF_8);
        final Path file = Files.write(temporary.resolve("content.csv"), content);
        final var attempts = new AtomicInteger();
        final var hashes = new CopyOnWriteArrayList<String>();
        final var paths = new CopyOnWriteArrayList<String>();
        final var owners = new CopyOnWriteArrayList<String>();
        final var conditions = new CopyOnWriteArrayList<String>();
        final var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                hashes.add(DigestUtils.sha256Hex(exchange.getRequestBody()));
                paths.add(exchange.getRequestURI().getPath());
                owners.add(exchange.getRequestHeaders().getFirst("x-amz-meta-" + UploadMetadata.OWNER));
                conditions.add(exchange.getRequestHeaders().getFirst("If-None-Match"));
                if (attempts.incrementAndGet() == 1) {
                    byte[] error = "<Error><Code>InternalError</Code><Message>synthetic retry</Message></Error>"
                            .getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(503, error.length);
                    exchange.getResponseBody().write(error);
                } else {
                    exchange.getResponseHeaders().add("ETag", "\"" + DigestUtils.md5Hex(content) + "\"");
                    exchange.sendResponseHeaders(200, -1);
                }
            }
        });
        server.start();
        try (var s3 = S3Client.builder().endpointOverride(URI.create("http://127.0.0.1:" + server.getAddress().getPort()))
                .region(Region.US_EAST_1).forcePathStyle(true)
                .serviceConfiguration(S3Configuration.builder().chunkedEncodingEnabled(false).build())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("synthetic", "synthetic")))
                .overrideConfiguration(config -> config.retryPolicy(RetryPolicy.builder().numRetries(1).build())
                        .apiCallTimeout(Duration.ofSeconds(15))).build()) {
            final var config = new S3Config() { @Override public String getS3Bucket() { return "uploads"; } };
            final var storage = new StorageServiceS3Impl(config, s3);
            final String key = storage.putUpload("content.csv", file, owner);
            assertEquals(2, attempts.get());
            assertEquals(java.util.List.of(DigestUtils.sha256Hex(content), DigestUtils.sha256Hex(content)), hashes);
            assertEquals(java.util.List.of("/uploads/" + key, "/uploads/" + key), paths);
            assertEquals(java.util.List.of(owner, owner), owners);
            assertEquals(java.util.List.of("*", "*"), conditions);
        } finally {
            server.stop(0);
        }
    }
}
