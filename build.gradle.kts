plugins {
    id("java-library")
    id("maven-publish")
    id("io.papermc.paperweight.userdev") version "1.7.7"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("io.freefair.lombok") version "8.12.1"
}

group = "eu.decentsoftware.holograms"
version = "2.8.12"
description = "A lightweight yet very powerful hologram plugin with many features and configuration options."

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    paperweight.paperDevBundle("1.21.3-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.2")

    compileOnly("com.github.cryptomorin:XSeries:13.0.0")
    compileOnly("de.tr7zw:item-nbt-api-plugin:2.14.1")

    compileOnly("org.jetbrains:annotations:26.0.2")
    annotationProcessor("org.jetbrains:annotations:26.0.2")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

java {
    withJavadocJar()
    withSourcesJar()
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
        (options as StandardJavadocDocletOptions).tags("implNote")
    }

    processResources {
        filteringCharset = Charsets.UTF_8.name()
        val props = mapOf("version" to project.version.toString(), "description" to project.description.toString())
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveClassifier.set("")
    }
}