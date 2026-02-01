import xyz.jpenilla.resourcefactory.paper.PaperPluginYaml
import xyz.jpenilla.runtask.task.AbstractRun

plugins {
    java
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "9.3.1"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("org.checkerframework") version "0.6.61"
    id("com.diffplug.spotless") version "8.2.0"
    id("xyz.jpenilla.resource-factory-paper-convention") version "1.3.1"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
}

apply(plugin = "org.checkerframework")

group = "me.brandonli"
version = "1.21.11-v1.0.0"
description = "MineSwap"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Core")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit") { isTransitive = false }
    implementation(platform("com.intellectualsites.bom:bom-newest:1.55"))
    implementation("io.javalin:javalin:6.7.0")
    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

val targetJavaVersion = 25
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    val language = JavaLanguageVersion.of(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    toolchain.languageVersion.set(language)
}

tasks.withType<AbstractRun>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        vendor = JvmVendorSpec.JETBRAINS
        languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    })
    jvmArgs("-Xmx2040m", "-XX:+AllowEnhancedClassRedefinition", "-XX:+AllowRedefinitionToAddDeleteMethods")
}

paperPluginYaml {
    name = "MineSwap"
    version = "${project.version}"
    description = "When you can't afford RAM"
    authors = listOf("PulseBeat_02")
    apiVersion = "1.21"
    prefix = "MineSwap"
    main = "me.brandonli.mineswap.MineSwap"
    dependencies.server("WorldEdit", PaperPluginYaml.Load.BEFORE, true)
}

tasks {

    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    withType<Test> {
        failOnNoDiscoveredTests = false
    }

    withType<JavaCompile>().configureEach {
        val set = setOf("-parameters", "-Xlint:deprecation", "-Xlint:unchecked")
        options.compilerArgs.addAll(set)
        options.encoding = "UTF-8"
        options.release.set(targetJavaVersion)
        options.isFork = true
        options.forkOptions.memoryMaximumSize = "4g"
        options.compilerArgs.addAll(listOf("-Xmaxerrs", "1000"))
        options.forkOptions.jvmArgs = (options.forkOptions.jvmArgs ?: mutableListOf()).apply {
            // Checker Framework Gradle Plugin forgot to include this argument for latest Checker Framework versions
            // See https://github.com/typetools/checker-framework/issues/7241
            // See https://github.com/kelloggm/checkerframework-gradle-plugin/blob/fe79a94a8399d097cf3e2e3e2ab0626e46bfbd4f/src/main/groovy/org/checkerframework/gradle/plugin/CheckerFrameworkPlugin.groovy#L384
            add("--add-exports=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED")
        }
    }

    assemble {
        dependsOn("shadowJar")
    }

    build {
        dependsOn("spotlessApply")
    }

    runServer {
        downloadPlugins {
            url("https://cdn.modrinth.com/data/z4HZZnLr/versions/MOe9fY3h/FastAsyncWorldEdit-Bukkit-2.15.0.jar")
        }
        minecraftVersion("1.21.11")
    }

    sourceSets {
        main {
            java.srcDir("src/main/java")
            resources.srcDir("src/main/resources")
        }
    }

    spotless {
        java {
            importOrder()
            removeUnusedImports()
            palantirJavaFormat().style("GOOGLE")
            licenseHeaderFile("HEADER")
        }
    }

    checkerFramework {
        checkers = listOf("org.checkerframework.checker.nullness.NullnessChecker")
        extraJavacArgs = listOf(
            "-AsuppressWarnings=uninitialized",
            "-Astubs=${project.file("checker-framework")}"
        )
    }
}
