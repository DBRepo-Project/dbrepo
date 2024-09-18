package at.tuwien.utils;

import java.io.File;
import java.io.IOException;

public class FileUtils {

    public static void delete(File file) throws IOException {
        if (file.exists()) {
            org.apache.commons.io.FileUtils.forceDelete(file);
        }
    }

}
