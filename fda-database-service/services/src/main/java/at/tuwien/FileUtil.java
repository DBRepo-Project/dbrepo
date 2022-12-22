package at.tuwien;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.InputStreamResource;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

public class FileUtil {

    /**
     * Loads a resource from the resource path when the application is compiled into a .jar and during runtime.
     *
     * @param path The path to the resource.
     * @return The text contents of the resource.
     * @throws IOException The resource could not be loaded.
     */
    public static String loadResource(String path) throws IOException {
        final InputStreamResource resource = new InputStreamResource(FileUtils.openInputStream(
                new File("src/test/resources/" + path)));
        return FileUtils.readFileToString(resource.getFile(), Charset.defaultCharset());
    }

}
