import io.papermc.hangarpublishplugin.model.Platforms
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default
import org.ajoberstar.grgit.Grgit
import xyz.jpenilla.runpaper.task.RunServer
import java.util.*

plugins {
    kotlin("jvm") version "1.9.23"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("com.modrinth.minotaur") version "2.+"
    id("org.jetbrains.changelog") version "2.2.0"
    id("olf.build-logic")
}
if (!File("$rootDir/.git").exists()) {
    logger.lifecycle(
        """
    **************************************************************************************
    You need to fork and clone this repository! Don't download a .zip file.
    If you need assistance, consult the GitHub docs: https://docs.github.com/get-started/quickstart/fork-a-repo
    **************************************************************************************
    """.trimIndent()
    ).also { System.exit(1) }
}

group = "dev.themeinerlp"
val minecraftVersion = "1.20.1"
val supportedMinecraftVersions = listOf(
    "1.16.5",
    "1.17",
    "1.17.1",
    "1.18",
    "1.18.1",
    "1.18.2",
    "1.19",
    "1.19.1",
    "1.19.2",
    "1.19.3",
    "1.19.4",
    "1.20",
    "1.20.1",
)

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$minecraftVersion-R0.1-SNAPSHOT")
    implementation("dev.themeinerlp:plugin-debug:1.1.0")
    implementation("dev.themeinerlp.plugin-debug:bukkit-extension:1.1.0")
    implementation("net.kyori:adventure-text-minimessage:4.16.0")


    // testing
    testImplementation(kotlin("test"))
    testImplementation("io.papermc.paper:paper-api:$minecraftVersion-R0.1-SNAPSHOT")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.19:3.1.0")
    testImplementation("io.mockk:mockk:1.13.10")
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }
}

bukkit {
    main = "dev.themeinerlp.attollo.Attollo"
    apiVersion = "1.16"
    authors = listOf("TheMeinerLP")

    permissions {
        register("attollo.use") {
            description = "Allows the player to use the plugin"
            default = Default.TRUE
        }
    }
    commands {
        register("attollo") {
            permission = "attollo.command.attollo"
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions {
            jvmTarget = "17"
        }
    }
    test {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    supportedMinecraftVersions.forEach { serverVersion ->
        register<RunServer>("run-$serverVersion") {
            minecraftVersion(serverVersion)
            jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true")
            group = "run paper"
            runDirectory.set(file("run-$serverVersion"))
            pluginJars(rootProject.tasks.shadowJar.map { it.archiveFile }.get())
        }
    }
    register<RunServer>("runFolia") {
        downloadsApiService.set(xyz.jpenilla.runtask.service.DownloadsAPIService.folia(project))
        minecraftVersion(minecraftVersion)
        group = "run paper"
        runDirectory.set(file("run-folia"))
        jvmArgs("-DPaper.IgnoreJavaVersion=true", "-Dcom.mojang.eula.agree=true")
    }
    generateBukkitPluginDescription {
        doLast {
            outputDirectory.file(fileName).get().asFile.appendText("folia-supported: true")
        }
    }
}

changelog {
    version.set(baseVersion)
    path.set("${project.projectDir}/CHANGELOG.md")
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}

val branch = rootProject.branchName()
val baseVersion = project.version as String
val isRelease = !baseVersion.contains('-')
val isMainBranch = branch == "master"
if (!isRelease || isMainBranch) { // Only publish releases from the main branch
    val suffixedVersion =
        if (isRelease) baseVersion else baseVersion + "+" + System.getenv("GITHUB_RUN_NUMBER")
    val changelogContent = if (isRelease) {
        "See [GitHub](https://github.com/OneLiteFeatherNET/BetterGoPaint) for release notes."
    } else {
        val commitHash = rootProject.latestCommitHash()
        "[$commitHash](https://github.com/OneLiteFeatherNET/BetterGoPaint/commit/$commitHash) ${rootProject.latestCommitMessage()}"
    }
    hangarPublish {
        publications.register("Attollo") {
            version.set(suffixedVersion)
            channel.set(if (isRelease) "Release" else if (isMainBranch) "Snapshot" else "Alpha")
            id.set("Attollo")
            changelog.set(changelogContent)

            apiKey = System.getenv("HANGAR_SECRET")

            platforms {
                register(Platforms.PAPER) {
                    jar.set(tasks.shadowJar.flatMap { it.archiveFile })
                    platformVersions.set(supportedMinecraftVersions)
                }
            }
        }
    }
    modrinth {
        token.set(System.getenv("MODRINTH_TOKEN"))
        projectId.set("ULt9SvKn")
        versionNumber.set(version.toString())
        versionType.set(System.getenv("MODRINTH_CHANNEL"))
        uploadFile.set(tasks.shadowJar as Any)
        gameVersions.addAll(supportedMinecraftVersions)
        loaders.add("paper")
        loaders.add("bukkit")
        loaders.add("folia")
        changelog.set(
            project.changelog.renderItem(
                project.changelog.getOrNull(baseVersion) ?: project.changelog.getUnreleased()
            )
        )
    }
}