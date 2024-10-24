package cn.xbhel.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InaccessibleObjectException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public final class FileUtils {

    private static final String CLASSPATH_PREFIX = "classpath:";

    private FileUtils() {
        throw new InaccessibleObjectException("Cannot create an object for the class.");
    }

    public static Path get(String path) {
        if(path.startsWith(CLASSPATH_PREFIX)) {
            URL url = Objects.requireNonNull(Thread.currentThread().getContextClassLoader()
                    .getResource(path.substring(CLASSPATH_PREFIX.length())));
            try {
                return Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
        return Paths.get(path);
    }

    public static InputStream newBufferStream(String path) throws IOException {
        return new BufferedInputStream(Files.newInputStream(get(path)));
    }

}
