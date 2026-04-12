package util;

import javafx.fxml.FXMLLoader;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

public final class FxmlUtil {

    private FxmlUtil() {
    }

    public static FXMLLoader loader(String resourcePath) {
        return new FXMLLoader(resourceUrl(resourcePath));
    }

    public static URL resourceUrl(String resourcePath) {
        URL classpathUrl = FxmlUtil.class.getResource(resourcePath);
        if (classpathUrl != null) {
            return classpathUrl;
        }

        String normalized = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
        Path fallbackPath = Path.of("resources", normalized);
        if (Files.exists(fallbackPath)) {
            try {
                return fallbackPath.toUri().toURL();
            } catch (Exception e) {
                throw new IllegalStateException("Khong the mo resource: " + resourcePath, e);
            }
        }

        throw new IllegalStateException(
            "Khong tim thay resource: " + resourcePath
            + ". Hay them thu muc resources vao classpath hoac copy resources vao bin.");
    }
}
