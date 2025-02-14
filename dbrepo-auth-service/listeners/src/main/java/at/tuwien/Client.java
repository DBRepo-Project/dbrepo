package at.tuwien;

import org.jboss.logging.Logger;

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
    private static final Logger log = Logger.getLogger(Client.class);

    public static void postService(String data) throws IOException {
        try {
            final String urlString = System.getenv("METADATA_SERVICE_ENDPOINT");
            log.debugf("METADATA_SERVICE_ENDPOINT: %s", urlString);
            if (urlString == null || urlString.isEmpty()) {
                throw new IllegalArgumentException("Environment variable METADATA_SERVICE_ENDPOINT is not set or is empty.");
            }
            final String systemUsername = System.getenv("SYSTEM_USERNAME");
            if (systemUsername == null || systemUsername.isEmpty()) {
                throw new IllegalArgumentException("Environment variable SYSTEM_USERNAME is not set or is empty.");
            }
            log.debugf("SYSTEM_USERNAME: %s", systemUsername);
            final String systemPassword = System.getenv("SYSTEM_PASSWORD");
            if (systemPassword == null || systemPassword.isEmpty()) {
                throw new IllegalArgumentException("Environment variable SYSTEM_PASSWORD is not set or is empty.");
            }
            final URL url = URI.create(urlString + "/api/user").toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            final String token = systemUsername + ":" + systemPassword;
            conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString(token.getBytes(
                    Charset.defaultCharset())));
            conn.setRequestProperty("Content-Type", "application/json; utf-8");

            OutputStream os = conn.getOutputStream();
            os.write(data.getBytes());
            os.flush();

            final int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_CREATED && responseCode != HttpURLConnection.HTTP_OK) {
                throw new RuntimeException("Failed : HTTP error code : " + responseCode);
            }

            final BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String output;
            log.debugf("Output from Server .... \n");
            while ((output = br.readLine()) != null) {
                System.out.println(output);
                log.debugf("Input from Server: %s", output);
            }
            conn.disconnect();
        } catch (IOException e) {
            throw new IOException("Failed to post service: " + e.getMessage(), e);
        }
    }
}
