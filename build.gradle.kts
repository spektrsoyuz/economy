import net.minecrell.pluginyml.bukkit.BukkitPluginDescription
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
    id("io.freefair.lombok") version "9.0.0"
    id("de.eldoria.plugin-yml.paper") version "0.8.0"
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
    implementation("org.spongepowered:configurate-hocon:4.3.0-SNAPSHOT")

    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("net.milkbowl.vault:VaultUnlockedAPI:2.15")
    compileOnly("com.palmergames.bukkit.towny:towny:0.101.2.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
    }
}

paper {
    name = "economy"
    version = project.version.toString()
    main = "com.spektrsoyuz.economy.EconomyPlugin"
    description = ""
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
        register("Towny") {
            load = PaperPluginDescription.RelativeLoadOrder.AFTER
            required = false
            joinClasspath = false
        }
    }

    permissions {
        register("economy.admin")
        register("economy.balance.other")
        register("economy.balance")
        register("economy.pay")

        register("economy.*") {
            this.description = "Grants access to all commands"
            this.default = BukkitPluginDescription.Permission.Default.OP
            this.children = listOf(
                "economy.admin",
                "economy.balance.other",
                "economy.balance",
                "economy.pay",
            )
        }
    }
}