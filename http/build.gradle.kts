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
    id("com.gradleup.shadow") version "9.3.2"
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

val archiveName = "nelumbo-http-server"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(project(":"))
    implementation(project(":lsp:server"))
    implementation("org.eclipse.lsp4j:org.eclipse.lsp4j:1.0.0")
    implementation("org.modelingvalue:immutable-collections:5.0.1-BRANCHED")
    implementation("io.javalin:javalin:6.3.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.0")
    runtimeOnly("org.slf4j:slf4j-simple:2.0.16")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-ea") // Enable assertions
}

val frontendDir = layout.projectDirectory.dir("src/main/frontend")

// Resolve the npm executable: the PATH first (CI/setup-node and normal shells), then nvm's node
// versions (a Gradle daemon started outside a login shell does not see nvm on the PATH).
fun findNpm(): String {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    val exe       = if (isWindows) "npm.cmd" else "npm"
    val onPath    = System.getenv("PATH").orEmpty().split(File.pathSeparator)
            .map { File(it, exe) }
    val nvmDir    = File(System.getProperty("user.home"), ".nvm/versions/node")
    val inNvm     = (nvmDir.listFiles()?.filter { it.isDirectory } ?: emptyList())
            .sortedByDescending { it.lastModified() }
            .map { File(it, "bin/$exe") }
    return (onPath + inNvm).firstOrNull { it.canExecute() }?.absolutePath ?: exe
}

val npmBundle by tasks.registering(Exec::class) {
    description = "install frontend deps and build the Monaco fields bundle"
    workingDir  = frontendDir.asFile
    commandLine(findNpm(), "run", "dist")
    inputs.dir(frontendDir.dir("src"))
    inputs.file(frontendDir.file("package.json"))
    inputs.file(frontendDir.file("package-lock.json"))
    inputs.file(frontendDir.file("tsconfig.json"))
    inputs.file(frontendDir.file("esbuild.mjs"))
    outputs.dir(frontendDir.dir("dist"))
}

val copyFrontend by tasks.registering(Sync::class) {
    dependsOn(npmBundle)
    from(frontendDir.dir("dist"))
    // source maps only help debug this bundle in devtools; no need to ship ~9 MB of them in the server jar
    exclude("*.map")
    into(layout.buildDirectory.dir("generated-resources/public/assets"))
}

// Register the copied bundle as a source-set OUTPUT dir (not a resources source dir): this puts it on the
// runtime/test classpath and into serverJar via sourceSets.main.output, carries the copyFrontend dependency,
// and - unlike resources.srcDir - keeps it out of the sourcesJar.
sourceSets.main {
    output.dir(mapOf("builtBy" to copyFrontend), layout.buildDirectory.dir("generated-resources"))
}

tasks.register<ShadowJar>("serverJar") {
    archiveBaseName.set(archiveName)
    // Produce a single shaded jar without the default "-all" classifier
    archiveClassifier.set("")
    manifest {
        attributes["Main-Class"] = "org.modelingvalue.nelumbo.http.Main"
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
    // Disable plain jar to avoid duplicate artifact name; we use the shaded jar as the main distribution
    enabled = false
}
