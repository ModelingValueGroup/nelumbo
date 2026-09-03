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
    id("com.gradleup.shadow") version "9.6.1"
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val archiveName = "nelumbo-cli"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
    implementation(libs.mvg.json)

    testImplementation(libs.junit.jupiter)
    // the test client parses/builds JSON with Jackson; the server itself uses mvg-json
    testImplementation(libs.jackson.databind)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-ea") // Enable assertions
}

tasks.register<ShadowJar>("cliJar") {
    archiveBaseName.set(archiveName)
    // Produce a single shaded jar without the default "-all" classifier
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "org.modelingvalue.nelumbo.cli.NelumboCli"
    }
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())

    // Exclude signature files from signed dependencies to avoid SecurityException
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    mergeServiceFiles()
}

tasks.shadowJar {
    // Disable default shadowJar task; use cliJar instead
    enabled = false
}

tasks.jar {
    // plain jar (classifier avoids clashing with the shaded cliJar); needed so other projects can depend on this one
    archiveClassifier.set("plain")
}
