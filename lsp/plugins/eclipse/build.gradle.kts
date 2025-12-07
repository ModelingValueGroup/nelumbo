
plugins {
    id("java")
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
}
tasks.jar {
    archiveBaseName.set("eclipse-nelumbo-plugin")
    manifest {
        attributes(
            mapOf(
                "Bundle-ManifestVersion" to "2",
                "Bundle-Name" to "nelumbo-lsp-eclipse",
                "Bundle-Version" to version,
                "Bundle-SymbolicName" to "eclipse;singleton:=true",
                "Bundle-Vendor" to "Modeling Value Group",
                "Bundle-RequiredExecutionEnvironment" to "JavaSE-21",
                "Require-Bundle" to listOf(
                    "org.eclipse.ui",
                    "org.eclipse.lsp4e",
                    "org.eclipse.core.contenttype"
                ).joinToString(",").trim(),
            )
        )
    }
}
