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
    id("com.gradleup.shadow") version "9.3.1"
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val archiveName = "nelumbo-lsp-server"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
    implementation("org.modelingvalue:immutable-collections:4.1.0-BRANCHED")
    implementation("org.ow2.asm:asm-tree:9.9.1")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:1.0.0")
    // Include websocket launchers so Main.start(ws) can find a WebSocket launcher at runtime
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.websocket.jakarta:1.0.0")
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j.websocket:1.0.0")
    // Jakarta WebSocket server (Tyrus)
    implementation("org.glassfish.tyrus:tyrus-server:2.2.2")
    implementation("org.glassfish.tyrus:tyrus-container-grizzly-server:2.2.2")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.21.0")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.21.0")
    implementation("org.tomlj:tomlj:1.1.1")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-ea") // Enable assertions
}

tasks.register<ShadowJar>("serverJar") {
    archiveBaseName.set(archiveName)
    // Produce a single shaded jar without the default "-all" classifier
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "org.modelingvalue.nelumbo.lsp.Main"
    }
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())

    // Exclude signature files from signed dependencies to avoid SecurityException
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.shadowJar {
    // Disable default shadowJar task; use serverJar instead
    enabled = false
}

tasks.jar {
    // Disable plain jar to avoid duplicate artifact name; we use the shaded jar as the main distribution
    enabled = false
}
