import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.2.2"
    id("io.freefair.lombok") version "9.1.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
}

group = "com.spektrsoyuz"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/creatorfromhell/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.spongepowered:configurate-hocon:4.3.0-SNAPSHOT")

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.16")
    compileOnly("io.github.miniplaceholders:miniplaceholders-api:3.0.1")
    compileOnly("me.clip:placeholderapi:2.11.7")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    runServer {
        minecraftVersion("1.21.8")
    }
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set("economy")
    }
}

paper {
    name = "economy"
    version = project.version.toString()
    main = "com.spektrsoyuz.economy.EconomyPlugin"
    description = "A lightweight modern economy plugin"
    apiVersion = "1.21.8"
    website = "https://spektrsoyuz.com"
    authors = listOf("SpektrSoyuz")
    foliaSupported = true

    serverDependencies {
        // Dependencies
        register("Vault") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = true
            joinClasspath = true
        }
        register("MiniPlaceholders") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
            joinClasspath = true
        }
        register("PlaceholderAPI") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
            joinClasspath = true
        }
        // Supported plugins
        register("Towny") {
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
            required = false
            joinClasspath = false
        }
        register("QuickShop-Hikari") {
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
            required = false
            joinClasspath = false
        }
    }

    permissions {
        register("economy.admin")
        register("economy.balance.all")
        register("economy.balance.other")
        register("economy.balance")
        register("economy.pay")

        register("economy.*") {
            this.description = "Grants access to all commands"
            this.default = BukkitPluginDescription.Permission.Default.OP
            this.children = listOf(
                "economy.admin",
                "economy.balance.all",
                "economy.balance.other",
                "economy.balance",
                "economy.pay",
            )
        }
    }
}