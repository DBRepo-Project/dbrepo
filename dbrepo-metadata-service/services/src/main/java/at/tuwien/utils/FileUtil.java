package at.tuwien.utils;

import lombok.extern.log4j.Log4j2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

@Log4j2
public class FileUtil {

    /**
     * Loads a resource from the resource path when the application is compiled into a .jar and during runtime.
     *
     * @param resourcePath The path to the resource.
     * @return The text contents of the resource.
     * @throws IOException The resource could not be loaded.
     */
    public static List<String> loadResource(String resourcePath) throws IOException {
        final InputStream inputStream = FileUtil.class.getResourceAsStream(resourcePath);
        if (inputStream == null) {
            log.error("Failed to load query store input stream file {}", resourcePath);
            throw new IOException("Failed to load query store input stream file");
        }
        final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        final List<String> lines = new LinkedList<>();
        while(reader.ready()) {
            lines.add(reader.readLine());
        }
        inputStream.close();
        reader.close();
        return lines;
    }

}
