package org.modelingvalue.nelumbo.lsp;

import static org.modelingvalue.nelumbo.lsp.U.findProjects;
import static org.modelingvalue.nelumbo.lsp.U.withProgress;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.tomlj.Toml;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlTable;

@SuppressWarnings("DuplicatedCode")
public class Workspace {
    private       Setting      setting      = new Setting();
    private final List<String> folders      = new ArrayList<>();
    private final List<Path>   dependencies = new ArrayList<>();

    public Workspace() {
        findSettings();
    }

    private void findSettings() {
        Path settingFile = U.getLocation(Main.class).getParent().resolve("settings.json");
        if (Files.exists(settingFile)) {
            setSetting(Setting.read(settingFile));
        } else {
            getSetting().save(settingFile);
        }
    }

    public Setting getSetting() {
        return setting;
    }

    public void setSetting(Setting setting) {
        this.setting = setting;
    }

    public List<String> getFolders() {
        return folders;
    }

    public void resolve() {
        List<Path> projects = folders.stream().flatMap(folder -> findProjects(Paths.get(URI.create(folder))).stream()).toList();

        Setting.Classpath classpath = setting.classpath();
        if (classpath.findConfiguration()) {
            findConfiguration(projects);
        }
        if (classpath.findOtherProject()) {
            indexClasses();
        }
        processSource(projects);
    }

    private void findConfiguration(List<Path> projects) {
        withProgress("Find Dependencies By Configuration", () -> {
            for (Path project : projects) {
                Path pomPath = project.resolve("pom.xml");
                if (Files.exists(pomPath)) {
                    try {
                        XmlMapper xmlMapper    = new XmlMapper();
                        JsonNode  pomTree      = xmlMapper.readTree(pomPath.toFile());
                        JsonNode  properties   = pomTree.get("properties");
                        JsonNode  dependencies = pomTree.get("dependencies");

                        if (dependencies != null) {
                            for (JsonNode dependency : dependencies) {
                                JsonNode groupIdNode    = dependency.get("groupId");
                                JsonNode artifactIdNode = dependency.get("artifactId");
                                JsonNode versionNode    = dependency.get("version");

                                if (groupIdNode == null || artifactIdNode == null || versionNode == null) {
                                    continue;
                                }

                                String groupId    = groupIdNode.asText();
                                String artifactId = artifactIdNode.asText();
                                String version    = versionNode.asText();

                                if (version.startsWith("${") && version.endsWith("}") && properties != null) {
                                    String   propertyName  = version.substring(2, version.length() - 1);
                                    JsonNode propertyValue = properties.get(propertyName);
                                    if (propertyValue != null) {
                                        version = propertyValue.asText();
                                    }
                                }

                                Path jarPath = Paths.get(System.getProperty("user.home")).resolve(".m2").resolve("repository").resolve(groupId.replace(".", "/")).resolve(artifactId).resolve(version).resolve(artifactId + "-" + version + ".jar");

                                if (Files.exists(jarPath)) {
                                    this.dependencies.add(jarPath);
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Error processing pom.xml: " + e.getMessage());
                    }
                }
            }

            for (String folder : folders) {
                Path folderPath = Paths.get(URI.create(folder));
                Path gradlePath = folderPath.resolve("gradle");

                if (Files.exists(gradlePath)) {
                    try (Stream<Path> walk = Files.walk(gradlePath)) {
                        walk.filter(path -> path.toString().endsWith(".toml")).forEach(tomlPath -> {
                            try {
                                TomlParseResult toml      = Toml.parse(tomlPath);
                                TomlTable       versions  = toml.getTable("versions");
                                TomlTable       libraries = toml.getTable("libraries");

                                if (libraries != null) {
                                    for (String key : libraries.keySet()) {
                                        TomlTable dependency = libraries.getTable(key);
                                        if (dependency == null) {
                                            continue;
                                        }

                                        String module = dependency.getString("module");
                                        if (module == null) {
                                            String group = dependency.getString("group");
                                            String name  = dependency.getString("name");
                                            if (group != null && name != null) {
                                                module = group + ":" + name;
                                            }
                                        }

                                        if (module == null) {
                                            continue;
                                        }

                                        String group = module.substring(0, module.indexOf(':'));
                                        String name  = module.substring(module.indexOf(':') + 1);

                                        String version = dependency.getString("version.ref");
                                        if (version != null && versions != null) {
                                            version = versions.getString(version);
                                        } else {
                                            version = dependency.getString("version");
                                        }

                                        if (version == null) {
                                            continue;
                                        }

                                        Path gradleCachePath = Paths.get(System.getProperty("user.home")).resolve(".gradle").resolve("caches").resolve("modules-2").resolve("files-2.1").resolve(group).resolve(name).resolve(version);

                                        if (Files.exists(gradleCachePath)) {
                                            try (Stream<Path> cacheWalk = Files.walk(gradleCachePath)) {
                                                final String   version_ = version;
                                                Optional<Path> jarFile  = cacheWalk.filter(path -> path.getFileName().toString().equals(name + "-" + version_ + ".jar")).findFirst();

                                                if (jarFile.isPresent() && Files.exists(jarFile.get())) {
                                                    this.dependencies.add(jarFile.get());
                                                }
                                            } catch (IOException e) {
                                                System.err.println("Error walking gradle cache: " + e.getMessage());
                                            }
                                        }
                                    }
                                }
                            } catch (IOException e) {
                                System.err.println("Error parsing TOML file: " + e.getMessage());
                            }
                        });
                    } catch (IOException e) {
                        System.err.println("Error walking gradle directory: " + e.getMessage());
                    }
                }
            }
        });
    }

    private void processSource(List<Path> projects) {
        withProgress("Process Source", () -> {
            try (ThreadPoolExecutor pool = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors() * 2, Runtime.getRuntime().availableProcessors() * 4, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>())) {
                projects.forEach(project -> System.err.println(" - " + project));
            }
        });
    }

    private List<Path> findClasspath() {
        List<Path> result = new ArrayList<>(dependencies);

        for (String folder : folders) {
            result.addAll(U.findClasspath(Paths.get(URI.create(folder))));
        }

        for (String folder : folders) {
            for (Path project : U.findProjects(Paths.get(URI.create(folder)))) {
                result.addAll(U.findClasspath(project));
            }
        }

        return result;
    }

    public void indexClasses() {
        withProgress("Index Classes", () -> {
            List<Path> classpath = new ArrayList<>(findClasspath());
            try {
                classpath.add(Paths.get(Main.class.getProtectionDomain().getCodeSource().getLocation().toURI()));
            } catch (Exception e) {
                System.err.println("Error getting main class location: " + e.getMessage());
            }

            int                     classes    = 0;
            Map<Path, List<String>> classNames = U.findClassNames(classpath);
            for (Map.Entry<Path, List<String>> entry : classNames.entrySet()) {
                Path         path  = entry.getKey();
                List<String> names = entry.getValue();
                classes++;
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Workspace workspace = (Workspace) o;
        return Objects.equals(setting, workspace.setting) && Objects.equals(folders, workspace.folders) && Objects.equals(dependencies, workspace.dependencies);
    }

    @Override
    public int hashCode() {
        return Objects.hash(setting, folders, dependencies);
    }

    @Override
    public String toString() {
        return "Workspace{" + "setting=" + setting + ", folders=" + folders + ", dependencies=" + dependencies + '}';
    }
}
