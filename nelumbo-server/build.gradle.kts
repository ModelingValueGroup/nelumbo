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

val archiveName = "nelumbo-cli-server"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
    implementation("org.modelingvalue:immutable-collections:5.0.1-BRANCHED")
    implementation("org.modelingvalue:mvg-json:6.0.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
    // the test client parses/builds JSON with Jackson; the server itself uses mvg-json
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.22.1")
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
        attributes["Main-Class"] = "org.modelingvalue.nelumbo.server.Main"
    }
    from(sourceSets.main.get().output)
    configurations = listOf(project.configurations.runtimeClasspath.get())

    // Exclude signature files from signed dependencies to avoid SecurityException
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
    mergeServiceFiles()
}

tasks.shadowJar {
    // Disable default shadowJar task; use serverJar instead
    enabled = false
}

tasks.jar {
    // plain jar (classifier avoids clashing with the shaded serverJar); needed so other projects can depend on this one
    archiveClassifier.set("plain")
}
