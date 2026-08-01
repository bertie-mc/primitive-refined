import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.api.tasks.testing.Test

plugins {
    `java-library`
    idea
    id("net.neoforged.moddev") version "2.0.134"
}

val minecraft_version: String by project
val minecraft_version_range: String by project
val neo_version: String by project
val neo_version_range: String by project
val loader_version_range: String by project
val parchment_mappings_version: String by project
val parchment_minecraft_version: String by project
val mod_id: String by project
val mod_name: String by project
val mod_license: String by project
val mod_version: String by project
val mod_group_id: String by project
val mod_authors: String by project
val mod_description: String by project
val create_version: String by project
val ponder_version: String by project
val flywheel_version: String by project
val refined_storage_version: String by project

version = mod_version
group = mod_group_id

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://maven.createmod.net") } // Create, Ponder (+ Catnip), Flywheel
    maven { url = uri("https://api.modrinth.com/maven") }
}

base {
    archivesName = mod_id
}

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

val mockitoAgent by configurations.creating

neoForge {
    version = neo_version

    parchment {
        mappingsVersion = parchment_mappings_version
        minecraftVersion = parchment_minecraft_version
    }

    runs {
        register("client") {
            client()
        }
        register("server") {
            server()
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.DEBUG
        }
    }

    mods {
        register(mod_id) {
            sourceSet(sourceSets.main.get())
        }
    }

    unitTest {
        enable()
        testedMod = mods.getByName(mod_id)
    }
}

dependencies {
    // Create is a hard dependency: every block in this mod is a kinetic block and the
    // whole point of the addon is that it runs on stress instead of energy. It stays
    // compileOnly because the pack already ships Create - we compile against its API
    // and never bundle a byte of it.
    //
    // ":slim" is Create without its jar-in-jar payload. The payload is exactly the
    // three artifacts below, declared explicitly at the versions Create 6.0.10 pins.
    compileOnly("com.simibubi.create:create-1.21.1:$create_version:slim") {
        isTransitive = false
    }

    // Needed to compile, not merely to run: KineticBlockEntity -> SmartBlockEntity
    // implements net.createmod.ponder.api.VirtualBlockEntity, so javac cannot resolve
    // the supertype chain without ponder on the classpath. Catnip rides along inside
    // this jar; there is no standalone catnip artifact for 1.21.1.
    compileOnly("net.createmod.ponder:ponder-neoforge:$ponder_version") {
        isTransitive = false
    }

    // The full flywheel artifact, not -api-: SimpleBlockEntityVisualizer lives in
    // dev.engine_room.flywheel.lib, which the api-only jar does not carry. Registering a
    // visual is what makes the shaft spin *and* what stops its static blockstate model
    // being drawn on top of the spinning one.
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-1.21.1:$flywheel_version") {
        isTransitive = false
    }

    // Refined Storage, also a hard dependency. The storage half of this mod is not a
    // lookalike of RS - it is RS: our blocks are RS network nodes, our grids drive RS's
    // own container menus and screens, and the External Reader is an RS external storage.
    // Only the topology is ours, and it is the arcanetic kinetic graph.
    //
    // Modrinth's maven, because Refined Storage 2 publishes no artifact to Maven Central
    // under a coordinate that resolves for 1.21.1. `refined-storage` is the project slug;
    // the version is the Modrinth version_number of the jar pinned in
    // packs/*/mods/refined-storage.pw.toml. Keep the two in step.
    compileOnly("maven.modrinth:refined-storage:$refined_storage_version") {
        isTransitive = false
    }

    testImplementation(platform("org.junit:junit-bom:6.1.2"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core:5.23.0")
    mockitoAgent("org.mockito:mockito-core:5.23.0") {
        isTransitive = false
    }
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("net.neoforged:testframework:$neo_version")
    testImplementation("maven.modrinth:refined-storage:$refined_storage_version") {
        isTransitive = false
    }
    testCompileOnly("com.simibubi.create:create-1.21.1:$create_version:slim") {
        isTransitive = false
    }
    testCompileOnly("net.createmod.ponder:ponder-neoforge:$ponder_version") {
        isTransitive = false
    }
    testCompileOnly("dev.engine-room.flywheel:flywheel-neoforge-1.21.1:$flywheel_version") {
        isTransitive = false
    }
    testRuntimeOnly("com.simibubi.create:create-1.21.1:$create_version") {
        isTransitive = false
    }

    // A dev-time `runClient` would additionally need the real Create jar on the
    // runtime classpath. It is deliberately absent: this project builds a jar for the
    // packwiz packs and is tested in a Prism instance, not in a Gradle run.
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    jvmArgs("-javaagent:${mockitoAgent.singleFile.absolutePath}")
}

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "neo_version" to neo_version,
        "neo_version_range" to neo_version_range,
        "loader_version_range" to loader_version_range,
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_license" to mod_license,
        "mod_version" to mod_version,
        "mod_authors" to mod_authors,
        "mod_description" to mod_description,
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
    into("build/generated/sources/modMetadata")
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)
