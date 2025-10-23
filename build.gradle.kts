//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
// (C) Copyright 2018-2025 Modeling Value Group B.V. (http://modelingvalue.org)                                        ~
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

defaultTasks("mvgCorrector", "test", "publish", "mvgTagger")

plugins {
    `java-library`
    `maven-publish`
    id("org.modelingvalue.gradle.mvgplugin") version "1.1.3"
    id("com.gradleup.shadow") version "9.2.2"
    idea
    eclipse
}

mvgcorrector {
    setHeaderUrl("file:header-template.txt")
//    forceHeaderCorrection = true
}

dependencies {
    implementation("org.modelingvalue:immutable-collections:4.1.0-BRANCHED")
    implementation("com.formdev:flatlaf:3.5.2")
}
tasks {
    shadowJar {
        archiveClassifier.set("all")
        doFirst {
            // Clean only previous shadow jars; leave regular publication jars intact
            val libsDir = layout.buildDirectory.dir("libs")
            libsDir.get().asFile.listFiles()
                ?.filter { f -> f.isFile && (f.name.endsWith("-all.jar") || f.name.contains("-all-")) }
                ?.forEach { it.delete() }
        }
    }
}

// Ensure `gradle clean` also removes the generated documentation site
// This is safe if the directory does not exist
tasks.named<Delete>("clean") {
    delete(file("docs/site"))
}

publishing {
    publications {
        create<MavenPublication>("nelumbo") {
            from(components["java"])
            // artifact(tasks.shadowJar)
        }
    }
}

tasks.register<Exec>("generate-slides") {
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
