package at.ac.tuwien.ifs.dbrepo.service;

import at.ac.tuwien.ifs.dbrepo.config.S3Config;
import at.ac.tuwien.ifs.dbrepo.endpoints.UploadEndpoint;
import at.ac.tuwien.ifs.dbrepo.service.impl.StorageServiceS3Impl;
import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.http.*;
import org.apache.catalina.startup.Tomcat;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.multipart.support.StandardMultipartHttpServletRequest;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.lang.management.ManagementFactory;
import java.net.URI;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Runs the real upload endpoint and SDK behind Tomcat in a separate, small heap. */
public final class UploadHeapServer {
    static final String OWNER = "b3468012-3375-4d80-b6b0-58b72f96594f";

    public static void main(String[] args) throws Exception {
        final var client = S3Client.builder().endpointOverride(URI.create(args[0])).region(Region.US_EAST_1)
                .forcePathStyle(true).credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("seaweedfsadmin", "seaweedfsadmin"))).build();
        final var config = new S3Config() { @Override public String getS3Bucket() { return args[1]; } };
        final var endpoint = new UploadEndpoint(new StorageServiceS3Impl(config, client));
        final var caller = new JwtAuthenticationToken(Jwt.withTokenValue("synthetic")
                .header("alg", "RS256").subject(OWNER).build(), List.of(new SimpleGrantedAuthority("upload-file")));
        final var active = new AtomicInteger();
        final var largeReady = new AtomicBoolean();
        final var release = new CountDownLatch(1);
        final var peak = new AtomicLong();
        final var sampler = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon().factory());
        sampler.scheduleAtFixedRate(() -> peak.accumulateAndGet(
                ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(), Math::max), 0, 10, TimeUnit.MILLISECONDS);
        final Path base = Path.of(args[2]);
        final Path parts = Files.createDirectory(base.resolve("parts"));
        final var tomcat = new Tomcat();
        tomcat.setBaseDir(base.resolve("tomcat").toString());
        tomcat.setPort(0);
        tomcat.getConnector().setProperty("address", "127.0.0.1");
        tomcat.getConnector().setMaxPostSize(320 * 1024 * 1024);
        final var context = tomcat.addContext("", base.toString());
        final var upload = Tomcat.addServlet(context, "upload", new HttpServlet() {
            @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
                active.incrementAndGet();
                StandardMultipartHttpServletRequest multipart = null;
                try {
                    multipart = new StandardMultipartHttpServletRequest(request);
                    final var file = multipart.getFile("file");
                    if (file != null && file.getSize() > 1024 * 1024) {
                        largeReady.set(true);
                        if (!release.await(30, TimeUnit.SECONDS)) throw new IllegalStateException("Large upload was not released");
                    }
                    final var result = endpoint.create(file, caller);
                    response.setStatus(result.getStatusCode().value());
                    result.getHeaders().forEach((key, values) -> values.forEach(value -> response.addHeader(key, value)));
                } catch (Exception error) {
                    response.setStatus(503);
                    response.getWriter().write(error.getClass().getSimpleName());
                } finally {
                    if (multipart != null) new StandardServletMultipartResolver().cleanupMultipart(multipart);
                    active.decrementAndGet();
                }
            }
        });
        upload.setMultipartConfigElement(new MultipartConfigElement(parts.toString(), 300L * 1024 * 1024,
                320L * 1024 * 1024, 0));
        context.addServletMappingDecoded("/api/v1/upload", "upload");
        Tomcat.addServlet(context, "state", new HttpServlet() {
            @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException {
                response.setContentType("application/json");
                response.getWriter().printf("{\"active\":%d,\"largeReady\":%s,\"heapMax\":%d,\"sampledHeapPeak\":%d}",
                        active.get(), largeReady.get(), Runtime.getRuntime().maxMemory(), peak.get());
            }
            @Override protected void doPost(HttpServletRequest request, HttpServletResponse response) {
                release.countDown();
                response.setStatus(204);
            }
        });
        context.addServletMappingDecoded("/state", "state");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try { tomcat.stop(); tomcat.destroy(); } catch (Exception ignored) { }
            sampler.shutdownNow();
            client.close();
        }));
        tomcat.start();
        System.out.println("UPLOAD_PORT=" + tomcat.getConnector().getLocalPort());
        tomcat.getServer().await();
    }
}
