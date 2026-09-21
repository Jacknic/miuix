// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

import org.gradle.api.plugins.catalog.VersionCatalogPlugin

plugins {
    `version-catalog`
    id("module.publication")
}

// Published coordinates for the generated catalog. The version must stay in sync with
// `BuildConfig.LIBRARY_VERSION` used by the library modules (module.publication.gradle.kts).
group = BuildConfig.LIBRARY_ID
version = providers.gradleProperty("miuixVersion")
    .getOrElse(BuildConfig.LIBRARY_VERSION)

// A hack to prevent all projects evaluation if version catalog generation wasn't requested.
// Issue: https://github.com/gradle/gradle/issues/33568
gradle.taskGraph.whenReady {
    if (allTasks.any { it.name == VersionCatalogPlugin.GENERATE_CATALOG_FILE_TASKNAME }) {
        catalog.versionCatalog {
            configureVersionCatalog(
                group = project.group.toString(),
                version = project.version.toString(),
                publishedProjects = publishedMiuixModules,
            )
        }
    }
}

// Publish the generated catalog as a Maven artifact so external consumers can pull it via
// `dependencyResolutionManagement.versionCatalogs { create("miuix"); from("top.yukonga.miuix.kmp:miuix-version-catalog:...") }`.
publishing {
    publications {
        create<MavenPublication>("versionCatalog") {
            from(components["versionCatalog"])
            pom {
                name.set(project.name)
                description.set("Version catalog for Miuix")
            }
        }
    }
}

// The library modules miuix publishes. Each becomes a `miuix.<alias>` library entry that
// external consumers can pull via `implementation(miuix.ui)` / `implementation(miuix.core)` etc.
// Internal modules depend on siblings through typesafe project accessors, not this catalog.
val publishedMiuixModules = setOf(
    "miuix-ui",
    "miuix-core",
    "miuix-preference",
    "miuix-nav",
    "miuix-blur",
    "miuix-shader",
    "miuix-icons",
    "miuix-squircle",
)

fun VersionCatalogBuilder.configureVersionCatalog(
    group: String,
    version: String,
    publishedProjects: Set<String>,
) {
    // Versions
    val versionAlias = version("miuix", version)

    // Libraries
    for (projectName in publishedProjects) {
        library(
            /* alias = */ libraryAlias(projectName),
            /* group = */ group,
            /* artifact = */ projectName,
        ).versionRef(versionAlias)
    }
}

/**
 * Generates a library alias for a given module name to use in the version catalog.
 *
 * Miuix module names are flat (`miuix-ui`, `miuix-core`, ...); we strip the `miuix-` prefix so
 * `miuix-ui` becomes `ui`, yielding `miuix.ui` in the consumer's catalog.
 */
fun libraryAlias(projectName: String): String {
    return projectName.removePrefix("miuix-")
}
