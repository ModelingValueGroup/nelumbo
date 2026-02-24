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

defaultTasks("mvgCorrector", "test", "publish", "mvgTagger", "editorJar")

plugins {
    `java-library`
    `maven-publish`
    id("org.modelingvalue.gradle.mvgplugin") version "2.3.11"
    id("com.gradleup.shadow") version "9.3.1"
    idea
    eclipse
}

mvgcorrector {
    setHeaderUrl("file:header-template.txt")
//    forceHeaderCorrection = true
}

dependencies {
    implementation("org.modelingvalue:immutable-collections:4.1.0-BRANCHED")
    implementation("com.formdev:flatlaf:3.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    register<ShadowJar>("editorJar") {
        archiveClassifier.set("editor")
        manifest {
            attributes["Main-Class"] = "org.modelingvalue.nelumbo.tools.NelumboEditor"
        }
        from(sourceSets.main.get().output)
        configurations = listOf(project.configurations.runtimeClasspath.get())

        // Exclude signature files from signed dependencies to avoid SecurityException
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

        doFirst {
            // Clean only previous shadow jars; leave regular publication jars intact
            val libsDir = layout.buildDirectory.dir("libs")
            libsDir.get().asFile.listFiles()
                ?.filter { f -> f.isFile && (f.name.endsWith("-editor.jar") || f.name.contains("-editor-")) }
                ?.forEach { it.delete() }
        }
    }

    shadowJar {
        // Disable default shadowJar task; use editorJar instead
        enabled = false
    }
}

publishing {
    publications {
        create<MavenPublication>("nelumbo") {
            from(components["java"])
            // artifact(tasks.shadowJar)
        }
    }
}

tasks.register<Exec>("build-slides") {
    val docsDir = file("${project.projectDir}/docs").absolutePath
    val projectRoot = project.projectDir.absolutePath

    val dockerCandidateFile = file("/opt/local/bin/docker")
    executable = if (dockerCandidateFile.exists() && dockerCandidateFile.canExecute()) dockerCandidateFile.absolutePath else "docker"
    args(
        "run",
        "--rm",
        "-v", "$docsDir:/work",         // Mount docs at /work (build output goes to /work/site)
        "-v", "$projectRoot:/project",  // Mount project root so ../src/... is reachable from /work
        "-v", "${gradle.gradleUserHomeDir}/caches/pip:/root/.cache/pip",
        "-w", "/work",
        "python:3.12-alpine",
        "sh", "-lc",
        """
        ln -s /project/src ../src 2>/dev/null || true; \
        pip install -q --disable-pip-version-check --root-user-action=ignore mkslides 2>&1 | grep -v 'Created wheel' || true; \
        mkslides build NELUMBO.md
        cp nelumbo.svg site/assets
        """
    )
}

tasks.named<Delete>("clean") {
    delete(file("docs/site"))
    delete(rootProject.layout.buildDirectory)
    delete(file("lsp/server/build"))
    delete(file("lsp/plugins/eclipse/build"))
    delete(file("lsp/plugins/intellij/build"))
}

tasks.test {
    dependsOn(":lsp:server:test")
}
