package at.ac.tuwien.ifs.dbrepo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Base64;

public class Client {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public static void postService(String data) throws IOException {
        try {
            final String urlString = System.getenv("METADATA_SERVICE_ENDPOINT");
            log.debug("env.METADATA_SERVICE_ENDPOINT: {}", urlString);
            if (urlString == null || urlString.isEmpty()) {
                throw new IllegalArgumentException("Environment variable METADATA_SERVICE_ENDPOINT is not set or is empty.");
            }
            final String systemUsername = System.getenv("SYSTEM_USERNAME");
            if (systemUsername == null || systemUsername.isEmpty()) {
                throw new IllegalArgumentException("Environment variable SYSTEM_USERNAME is not set or is empty.");
            }
            log.debug("env.SYSTEM_USERNAME: {}", systemUsername);
            final String systemPassword = System.getenv("SYSTEM_PASSWORD");
            if (systemPassword == null || systemPassword.isEmpty()) {
                throw new IllegalArgumentException("Environment variable SYSTEM_PASSWORD is not set or is empty.");
            }
            final URL url = URI.create(urlString + "/api/user").toURL();
            log.debug("url: {}", url);
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            final String token = systemUsername + ":" + systemPassword;
            conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(token.getBytes(
                    Charset.defaultCharset())));
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            final OutputStream os = conn.getOutputStream();
            os.write(data.getBytes());
            os.flush();
            final int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_CREATED && responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + responseCode);
            }
            final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output;
            while ((output = br.readLine()) != null) {
                log.debug("input from server: {}", output);
            }
            conn.disconnect();
        } catch (IOException e) {
            throw new IOException("Failed to post service: " + e.getMessage(), e);
        }
    }
}
