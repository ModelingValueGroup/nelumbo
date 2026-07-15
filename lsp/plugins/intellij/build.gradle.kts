//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2026 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
//                                                                                                                     ~
// Licensed under the GNU Lesser General Public License v3.0 (the 'License'). You may not use this file except in      ~
// compliance with the License. You may obtain a copy of the License at: https://choosealicense.com/licenses/lgpl-3.0  ~
// Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on ~
// an 'AS IS' BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the  ~
// specific language governing permissions and limitations under the License.                                          ~
//                                                                                                                     ~
// Maintainers:                                                                                                        ~
//     Wim Bast, Tom Brus                                                                                              ~
//                                                                                                                     ~
// Contributors:                                                                                                       ~
//     Victor Lap                                                                                                      ~
//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

plugins {
    java
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

repositories {
    mavenCentral()
    intellijPlatform {
        releases()
        marketplace()
        defaultRepositories()
    }
}

dependencies {
    // Core nelumbo library: NelumboLexer (the IntelliJ lexer that backs the PSI tree) runs
    // org.modelingvalue.nelumbo.syntax.Tokenizer so PSI tokens match LSP-side tokenization.
    implementation(project(":"))
    // Transitive of the line above (root declares it as implementation, so it isn't on our
    // compile classpath by default). The includeBuild in settings.gradle.kts substitutes any
    // requested version with the local checkout, so the version here doesn't matter much.
    implementation("org.modelingvalue:immutable-collections:6.0.0-BRANCHED")

    intellijPlatform {
        intellijIdea("2025.3.2")
        plugin("com.redhat.devtools.lsp4ij:0.19.1")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

intellijPlatform {
    // keep the IDE sandbox (runIde/test scratch data) inside the build dir instead of <root>/.intellijPlatform
    sandboxContainer = layout.buildDirectory.dir("idea-sandbox")
}

// the IntelliJ Platform Gradle Plugin unconditionally (re)creates <root>/.intellijPlatform; remove it
// again. It is created at configuration time of every build (hence the whenReady hook) and again when
// initializeIntellijPlatformPlugin executes (hence the finalizer). localPlatformArtifacts holds only
// ivy descriptors that the plugin rewrites on demand, so it is deleted recursively; anything else
// (the coroutines javaagent downloaded for runIde/test) is left alone, in which case the rmdir of
// the root fails silently and the dir stays.
val platformCacheDir = rootProject.layout.projectDirectory.dir(".intellijPlatform").asFile
fun removePlatformCacheDir() {
    File(platformCacheDir, "localPlatformArtifacts").deleteRecursively()
    platformCacheDir.delete()
}
gradle.taskGraph.whenReady {
    removePlatformCacheDir()
}
val removeEmptyPlatformCacheDir by tasks.registering {
    doLast {
        removePlatformCacheDir()
    }
}
tasks.named("initializeIntellijPlatformPlugin") {
    finalizedBy(removeEmptyPlatformCacheDir)
}

tasks {
    patchPluginXml {
        sinceBuild.set("232")
        untilBuild.set("262.*")
        version = project.version.toString()
    }

    // Disable building searchable options to avoid starting a full IDE instance during build
    named("buildSearchableOptions") {
        enabled = false
    }

    // Disable bytecode instrumentation: we don't use IntelliJ form designer (.form) files or
    // @NotNull runtime checks. The Javac2 Ant task it relies on also breaks on non-JBR JDKs
    // (it scans <JAVA_HOME>/Packages, which only exists in a JetBrains Runtime).
    named("instrumentCode") {
        enabled = false
    }

    // Ensure the server fat jar is built and include it into resources as server.jar
    // Some versions of gradle-intellij-plugin repackage resources differently, so we also copy
    // the server jar directly into src/main/resources before build.
    val prepareServerResource by registering(Copy::class) {
        dependsOn(":lsp:server:serverJar")
        // Always run to ensure server.jar is refreshed and overwrites any existing file
        outputs.upToDateWhen { false }
        // Proactively delete any existing server.jar to avoid stale copies
        doFirst {
            delete("src/main/resources/server.jar")
        }
        from(project(":lsp:server").layout.buildDirectory.dir("libs").get().asFileTree.matching { include("nelumbo-lsp-server-*.jar") })
        into("src/main/resources")
        // No matter the versioned name, place it as server.jar for runtime loader
        rename { "server.jar" }
        // Ensure duplicates don't abort the copy if multiple matches exist
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    named("processResources") {
        dependsOn(prepareServerResource)
    }

    configureEach {
        if (name == "sourcesJar") {
            dependsOn(prepareServerResource)
        }
    }

    // Ensure plugin XML patching runs after the server resource is prepared to avoid Gradle validation issues
    named("patchPluginXml") {
        dependsOn(prepareServerResource)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
        // Enable auto-reload for plugin development
        autoReload.set(true)

        // JVM arguments for development
        jvmArgs(
            "-Didea.auto.reload.plugins=true",
            "-Dide.plugins.snapshot.on.unload.fail=true"
        )
    }

    // Configure prepareSandbox to handle plugin updates
    prepareSandbox {
        // The sandbox will be prepared with latest compatible versions
    }

    named<Zip>("buildPlugin") {
        // Customize the distribution zip name (was: intellij-<version>.zip)
        archiveBaseName.set("nelumbo-intellij-plugin")
        // Copying to Downloads is orchestrated at the root level after the whole build finishes.
    }
}
