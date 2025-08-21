import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.1"
    id("io.freefair.lombok") version "8.14"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("de.eldoria.plugin-yml.paper") version "0.7.1"
}

group = "com.spektrsoyuz"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/")
    maven("https://repo.glaremasters.me/repository/towny/")
}

dependencies {
    implementation("com.zaxxer:HikariCP:7.0.1")
    implementation("redis.clients:jedis:6.1.0")
    implementation("org.spongepowered:configurate-yaml:4.3.0-SNAPSHOT")

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.15")
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.2.0")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:2.3.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    shadowJar {
        archiveClassifier.set("")
    }
    build {
        dependsOn(shadowJar)
    }
}

paper {
    name = "economy"
    main = "com.spektrsoyuz.economy.EconomyPlugin"
    apiVersion = "1.21.8"
    website = "https://spektrsoyuz.com"
    authors = listOf("SpektrSoyuz")
    foliaSupported = false

    serverDependencies {
        register("Vault") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
            joinClasspath = true
        }
        register("MiniPlaceholders") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
            joinClasspath = true
        }
        register("Towny") {
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
            required = false
            joinClasspath = false
        }
    }

    permissions {
        register("economy.balance")

        register("economy.*") {
            this.description = "Grants access to all commands"
            this.default = BukkitPluginDescription.Permission.Default.OP
            this.children = listOf(
                "economy.balance",
            )
        }
    }
}