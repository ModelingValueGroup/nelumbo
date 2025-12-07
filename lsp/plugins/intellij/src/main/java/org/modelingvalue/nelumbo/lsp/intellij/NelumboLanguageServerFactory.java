package org.modelingvalue.nelumbo.lsp.intellij;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.project.Project;
import com.redhat.devtools.lsp4ij.LanguageServerFactory;
import com.redhat.devtools.lsp4ij.client.LanguageClientImpl;
import com.redhat.devtools.lsp4ij.server.JavaProcessCommandBuilder;
import com.redhat.devtools.lsp4ij.server.OSProcessStreamConnectionProvider;
import com.redhat.devtools.lsp4ij.server.StreamConnectionProvider;
import org.jetbrains.annotations.NotNull;
import org.modelingvalue.nelumbo.lsp.intellij.setting.PluginSetting;

public class NelumboLanguageServerFactory implements LanguageServerFactory {
    public static final String LATEST_ON_GITHUB_URL = "https://api.github.com/repos/ModelingValueGroup/nelumbo-lsp/releases/latest";

    {
        System.err.println("%%%% NelumboLanguageServerFactory");
    }

    @Override
    public StreamConnectionProvider createConnectionProvider(Project project) {
        System.err.println("%%%% createConnectionProvider");
        try {
            Path dir = Paths.get(System.getProperty("user.home")).resolve(Constants.ID);
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
            }
            Path localJarFile = dir.resolve("server.jar");
            if (Files.exists(localJarFile)) {
                Files.delete(localJarFile);
            }
            System.err.println("%%%% dir=" + dir + ", localJarFile=" + localJarFile);
            getJarFile(dir, localJarFile);
            List<String> command = makeCommand(project, localJarFile);
            System.err.println("%%%% command=" + command);
            return new OSProcessStreamConnectionProvider(new GeneralCommandLine(command));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getJarFile(Path dir, Path localJarFile) throws IOException {
        downloadLatestServerJar(dir, localJarFile);
        if (!Files.exists(localJarFile)) {
            // Fallback to embedded resource if downloading fails
            extractServerJar(dir, localJarFile);
        }
    }

    private void extractServerJar(Path dir, Path localJarFile) throws IOException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("server.jar")) {
            if (inputStream != null) {
                try (OutputStream outputStream = Files.newOutputStream(localJarFile)) {
                    inputStream.transferTo(outputStream);
                }
            }
        }
        if (!Files.exists(localJarFile)) {
            throw new RuntimeException("Local server jar not found");
        }
    }

    private void downloadLatestServerJar(Path dir, Path localJarFile) {
        // Query GitHub Releases API for the latest release
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).followRedirects(HttpClient.Redirect.NORMAL).build()) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(LATEST_ON_GITHUB_URL))//
                                             .header("Accept", "application/vnd.github+json")//
                                             .header("User-Agent", "nelumbo-intellij-plugin")//
                                             .timeout(Duration.ofSeconds(30))//
                                             .GET()//
                                             .build();
            HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.err.println("%%%% downloadLatestServerJar: statusCode=" + resp.statusCode());
            if (resp.statusCode() == 200) {
                System.err.println("%%%% body=" + resp.body());
                JsonObject           json     = JsonParser.parseString(resp.body()).getAsJsonObject();
                String               tag      = json.has("tag_name") && !json.get("tag_name").isJsonNull() ? json.get("tag_name").getAsString() : "latest";
                JsonArray            assets   = json.has("assets") && json.get("assets").isJsonArray() ? json.getAsJsonArray("assets") : new JsonArray();
                Optional<JsonObject> assetOpt = findJarAsset(assets);
                System.err.println("%%%% downloadLatestServerJar: got asset=" + assetOpt.isPresent());
                if (assetOpt.isPresent()) {
                    String downloadUrl       = assetOpt.get().get("browser_download_url").getAsString();
                    Path   cachedVersionFile = dir.resolve("server-" + tag + ".jar");
                    System.err.println("%%%% downloadLatestServerJar: cache exists=" + Files.exists(cachedVersionFile));
                    if (!Files.exists(cachedVersionFile)) {
                        // Download to temp file first
                        Path tmp = Files.createTempFile(dir, "server-", ".jar.part");
                        HttpRequest dlReq = HttpRequest.newBuilder(URI.create(downloadUrl))//
                                                       .header("User-Agent", "nelumbo-intellij-plugin")//
                                                       .timeout(Duration.ofMinutes(2))//
                                                       .GET()//
                                                       .build();
                        HttpResponse<Path> dlResp = client.send(dlReq, HttpResponse.BodyHandlers.ofFile(tmp));
                        System.err.println("%%%% downloadLatestServerJar: dlResp=" + dlResp.statusCode());
                        if (dlResp.statusCode() == 200) {
                            Files.move(tmp, cachedVersionFile);// Move atomically
                        }
                    }
                    tryLinkOrCopy(cachedVersionFile, localJarFile);
                }
            }
        } catch (Exception e) {
            // best effort; not fatal
            System.err.println("%%%% downloadLatestServerJar: Exception=" + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    private static void tryLinkOrCopy(Path from, Path to) {
        if (Files.exists(from)) {
            try {
                try {
                    Files.createSymbolicLink(to, from.getFileName());
                } catch (UnsupportedOperationException |
                         IOException |
                         SecurityException e) {
                    // If symlinks are not allowed, copy instead
                    Files.copy(from, to);
                }
            } catch (Exception ignore) {
                // best effort; not fatal
            }
        }
    }

    private static Optional<JsonObject> findJarAsset(JsonArray assets) {
        // find the published shaded server jar 'nelumbo-lsp-server-<version>.jar'
        return stream(assets)//
                             .filter(a -> a.has("name"))//
                             .filter(a -> a.get("name").getAsString().matches("nelumbo-lsp-server-[\\d.]+[.]jar"))//
                             .findFirst();
    }

    private static java.util.stream.Stream<JsonObject> stream(JsonArray array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsJsonObject);
    }

    private static @NotNull List<String> makeCommand(Project project, Path localJarFile) {
        PluginSetting.Setting settings = PluginSetting.getInstance().getState();
        List<String>          command;
        if (settings != null && settings.useJetBrainsRuntime) {
            JavaProcessCommandBuilder b = new JavaProcessCommandBuilder(project, Constants.SERVER_ID);
            b.setCp(localJarFile.toAbsolutePath().toString());
            command = b.create();
            // lsp4ij's JavaProcessCommandBuilder.create() returns command up to 'java -cp ...'
            command.add("org.modelingvalue.nelumbo.lsp.Main");
        } else {
            command = List.of("java", "-cp", localJarFile.toAbsolutePath().toString(), "org.modelingvalue.nelumbo.lsp.Main");
        }
        return command;
    }

    @Override
    public LanguageClientImpl createLanguageClient(Project project) {
        return new LanguageClientImpl(project) {
            final Gson gson = new GsonBuilder()//
                                               .setFieldNamingStrategy(FieldNamingPolicy.UPPER_CAMEL_CASE)//
                                               .create();

            @Override
            public Object createSettings() {
                return gson//
                           .toJsonTree(PluginSetting.getInstance().getState())//
                           .getAsJsonObject();
            }
        };
    }
}
