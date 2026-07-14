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

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("com.gradleup.shadow") version "9.5.1"
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val archiveName = "nelumbo-mcp-server"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
    implementation("io.modelcontextprotocol.sdk:mcp:2.0.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.1")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16") // logs to stderr; stdout is the protocol channel

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-ea")
}

// Bundle the language documentation into the jar for the search_docs tool, plus an
// index.txt so it can be enumerated from the classpath at runtime.
val copyDocs by tasks.registering(Sync::class) {
    from(rootProject.layout.projectDirectory.dir("docs")) {
        include("**/*.md")
        exclude("superpowers/**")
        exclude("site/**")
    }
    into(layout.buildDirectory.dir("generated-resources/nelumbo-docs"))
    doLast {
        val dir = destinationDir
        val index = dir.walkTopDown()
                .filter { it.isFile && it.extension == "md" }
                .map { it.relativeTo(dir).invariantSeparatorsPath }
                .sorted()
                .joinToString("\n")
        File(dir, "index.txt").writeText(index + "\n")
    }
}

// Registered as a source-set OUTPUT dir (same pattern as http's frontend bundle): on the
// runtime/test classpath and inside mcpJar, and kept out of any sources jar.
sourceSets.main {
    output.dir(mapOf("builtBy" to copyDocs), layout.buildDirectory.dir("generated-resources"))
}

tasks.register<ShadowJar>("mcpJar") {
    archiveBaseName.set(archiveName)
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "org.modelingvalue.nelumbo.mcp.Main"
    }
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    // mergeServiceFiles is REQUIRED: the MCP SDK discovers its JSON mapper via ServiceLoader
    mergeServiceFiles()
}

tasks.shadowJar {
    enabled = false
}

tasks.jar {
    enabled = false
}
