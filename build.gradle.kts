plugins {
    id("net.fabricmc.fabric-loom")
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm").version(kotlinVersion)
    kotlin("plugin.serialization").version(kotlinVersion)
}

base {
    val minecraftVersion: String by project
    val archivesBaseName: String by project
    archivesName.set("$archivesBaseName-mc$minecraftVersion")
}

val modVersion: String by project
version = modVersion
val mavenGroup: String by project
group = mavenGroup
val javaVersion: String by project

repositories {
    // CottonMC's maven (server.bbkr.space) is gone; LibGui jars come from its GitHub releases.
    exclusiveContent {
        forRepository {
            ivy("https://github.com/CottonMC/LibGui/releases/download/") {
                name = "LibGui GitHub releases"
                patternLayout { artifact("[revision]/[module]-[revision]+[classifier].[ext]") }
                metadataSources { artifact() }
            }
        }
        filter { includeModule("io.github.cottonmc", "LibGui") }
    }
    mavenCentral() // libninepatch (LibGui dependency, dev runtime)
    maven(url = "https://jitpack.io")  //Mahjong4j
    maven(url = "https://maven.shedaniel.me/") //Cloth Config
    maven(url = "https://maven.terraformersmc.com/") //Mod Menu
}

dependencies {
    // Minecraft 26.x is unobfuscated: no mappings, and mods are plain implementation dependencies.
    val minecraftVersion: String by project
    minecraft("com.mojang:minecraft:$minecraftVersion")
    val loaderVersion: String by project
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    val fabricVersion: String by project
    implementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    val fabricKotlinVersion: String by project
    implementation("net.fabricmc:fabric-language-kotlin:$fabricKotlinVersion")
    // Mahjong4J (https://github.com/mahjong4j/mahjong4j)
    include("com.github.mahjong4j:mahjong4j:0.3.2")?.let { implementation(it) }
    // LibGui (https://github.com/cottonmc/libgui)
    val libguiVersion: String by project
    val libguiMinecraftVersion: String by project
    include("io.github.cottonmc:LibGui:$libguiVersion:$libguiMinecraftVersion")?.let { implementation(it) }
    // Cloth Config API (https://modrinth.com/mod/cloth-config)
    val clothConfigVersion: String by project
    api("me.shedaniel.cloth:cloth-config-fabric:$clothConfigVersion") {
        exclude(group = "net.fabricmc.fabric-api")
    }
    // Mod Menu (https://modrinth.com/mod/modmenu)
    val modMenuVersion: String by project
    implementation("com.terraformersmc:modmenu:$modMenuVersion")

    // LibGui ships its own dependencies nested inside its jar, so players get them.
    // The dev environment doesn't load nested jars, and they have no live maven host,
    // so for runClient take them out of the LibGui release jar.
    localRuntime(files(libguiNestedJars()))

    testImplementation(kotlin("test"))
}

/** Extracts the jars nested in the LibGui release jar (except libninepatch, taken from Maven Central). */
fun libguiNestedJars(): List<File> {
    val libguiVersion: String by project
    val libguiMinecraftVersion: String by project
    val libgui = configurations.detachedConfiguration(
        dependencies.create("io.github.cottonmc:LibGui:$libguiVersion:$libguiMinecraftVersion")
    ).apply { isTransitive = false }.singleFile
    val outDir = layout.buildDirectory.dir("libgui-nested").get().asFile
    outDir.deleteRecursively()
    copy {
        from(zipTree(libgui)) { include("META-INF/jars/*.jar") }
        eachFile { path = name }
        includeEmptyDirs = false
        into(outDir)
    }
    return outDir.listFiles()?.sortedBy { it.name } ?: emptyList()
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
        options.release.set(javaVersion.toInt())
    }

    test { useJUnitPlatform() }

    jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }
    processResources {
        inputs.property("version", project.version)
        filesMatching("fabric.mod.json") { expand(mutableMapOf("version" to project.version)) }
    }
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toInt())) }
    withSourcesJar()
}

kotlin { jvmToolchain(javaVersion.toInt()) }
