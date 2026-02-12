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
    id("java")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}

val serverProject = project(":lsp:server")

// Generate MANIFEST.MF with proper OSGi formatting (avoid Gradle's 72-char line wrapping)
val generateManifest = tasks.register("generateManifest") {
    val manifestFile = layout.buildDirectory.file("osgi/MANIFEST.MF")
    outputs.file(manifestFile)
    doLast {
        manifestFile.get().asFile.parentFile.mkdirs()
        manifestFile.get().asFile.writeText(
            """
            |Manifest-Version: 1.0
            |Bundle-ManifestVersion: 2
            |Bundle-Name: Nelumbo LSP Eclipse Plugin
            |Bundle-SymbolicName: org.modelingvalue.nelumbo.lsp.eclipse;
            | singleton:=true
            |Bundle-Version: $version
            |Bundle-Vendor: Modeling Value Group
            |Bundle-RequiredExecutionEnvironment: JavaSE-21
            |Bundle-ActivationPolicy: lazy
            |Require-Bundle: org.eclipse.ui,
            | org.eclipse.ui.genericeditor,
            | org.eclipse.lsp4e,
            | org.eclipse.tm4e.registry,
            | org.eclipse.core.contenttype
            |
            """.trimMargin()
        )
    }
}

tasks.jar {
    archiveBaseName.set("eclipse-nelumbo-plugin")
    dependsOn(generateManifest)

    // Include the LSP server JAR as a resource (uses serverJar task, not jar)
    val serverJarTask = serverProject.tasks.named("serverJar")
    dependsOn(serverJarTask)
    from(serverJarTask.map { (it as org.gradle.jvm.tasks.Jar).archiveFile }) {
        rename { "server.jar" }
    }

    // Replace manifest after JAR creation to avoid Gradle's 72-char line wrapping
    doLast {
        val jarFile = archiveFile.get().asFile
        val manifestFile = generateManifest.get().outputs.files.singleFile
        ant.withGroovyBuilder {
            "jar"("destfile" to jarFile, "update" to true, "manifest" to manifestFile)
        }
        val downloads = File(System.getProperty("user.home"), "Downloads")
        if (downloads.isDirectory) {
            jarFile.copyTo(File(downloads, jarFile.name), overwrite = true)
            logger.lifecycle("Copied ${jarFile.name} to ${downloads}")
        }
    }
}
