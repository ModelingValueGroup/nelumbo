package org.modelingvalue.nelumbo.lsp.eclipse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

public class NelumboStreamConnectionProvider extends ProcessStreamConnectionProvider {
    public NelumboStreamConnectionProvider() throws IOException {
        var dir = Paths.get(System.getProperty("user.home"), "nelumbo-lsp");

        if (!dir.toFile().exists()) {
            Files.createDirectories(dir);
        }

        var localJarFile = dir.resolve("server.jar");

        try {
            if (Files.exists(localJarFile)) {
                Files.delete(localJarFile);
            }
            try (var inputStream = getClass().getClassLoader().getResourceAsStream("server.jar")) {
                assert inputStream != null;
                Files.copy(inputStream, localJarFile);
            }
        } catch (Exception ignored) {
            // ignored
        }

        if (!Files.exists(localJarFile)) {
            throw new IOException("Local server jar not found");
        }
        setCommands(List.of("java", "-cp", localJarFile.toFile().getAbsolutePath(), "org.modelingvalue.nelumbo.lsp.Main"));
    }
}
