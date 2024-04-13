plugins {
    id("idea")
    id("maven-publish")
    id("net.neoforged.gradle.userdev") version "7.0.81"
    id("java-library")
}

base {
    archivesName = "sodium-neoforge-1.20.4"
}

val MINECRAFT_VERSION: String by rootProject.extra
val MOD_VERSION: String by rootProject.extra

if (file("src/main/resources/META-INF/accesstransformer.cfg").exists()) {
    minecraft.accessTransformers {
        file("src/main/resources/META-INF/accesstransformer.cfg")
    }
}

jarJar.enable()

sourceSets {
    val service = create("service")

    service.apply {
        compileClasspath += main.get().compileClasspath
        compileClasspath += project(":common").sourceSets.getByName("workarounds").output
    }

    main.get().apply {
        compileClasspath += project(":common").sourceSets.getByName("workarounds").output
    }

    test.get().apply {
        compileClasspath += project(":common").sourceSets.getByName("workarounds").output
    }
}

repositories {
    exclusiveContent {
        forRepository {
            maven {
                url = uri("https://maven.pkg.github.com/ims212/forge-frapi")
                credentials {
                    username = "IMS212"
                    // Read only token
                    password = "ghp_" + "DEuGv0Z56vnSOYKLCXdsS9svK4nb9K39C1Hn"
                }
            }
        }
        filter {
            includeGroup("net.caffeinemc")
        }
    }
    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        //forRepositories(fg.repository) // Only add this if you're using ForgeGradle, otherwise remove this line
        filter {
            includeGroup("maven.modrinth")
        }
    }
}

val fullJar: Jar by tasks.creating(Jar::class) {
    duplicatesStrategy = DuplicatesStrategy.WARN
    dependsOn(tasks.jarJar)
    from(sourceSets.getByName("service").output)
    from(project(":common").sourceSets.getByName("desktop").output)
    from(project(":common").sourceSets.getByName("workarounds").output)

    into("META-INF/jarjar/") {
        from(tasks.jarJar.get().archiveFile)
    }

    into("META-INF") {
        from(projectDir.resolve("src").resolve("main").resolve("resources").resolve("sodium-icon.png"))

        from(projectDir.resolve("src").resolve("main").resolve("resources").resolve("META-INF").resolve("mods.toml"))
    }

    from(rootDir.resolve("LICENSE.md"))

    filesMatching("mods.toml") {
        expand(mapOf("version" to MOD_VERSION))
    }

    manifest.attributes["Main-Class"] = "net.caffeinemc.mods.sodium.desktop.LaunchWarn"
    manifest.attributes["FMLModType"] = "LIBRARY"

}

tasks.build {
    dependsOn(fullJar)
}

tasks.jar {
    from(rootDir.resolve("LICENSE.md"))

    archiveClassifier = "modonly"
}

runs {
    configureEach {
        modSource(project.sourceSets.main.get())
    }
    create("client") {
        dependencies {
            runtime("com.lodborg:interval-tree:1.0.0")
            runtime(project(":common").sourceSets.getByName("workarounds").output)
        }
        //environmentVariable("LD_PRELOAD", "/usr/lib/librenderdoc.so")
        environmentVariable("__GL_THREADED_OPTIMIZATIONS", "0")
    }

    create("data") {
        programArguments.addAll("--mod", "sodium", "--all", "--output", file("src/generated/resources/").getAbsolutePath(), "--existing", file("src/main/resources/").getAbsolutePath())
    }
}

dependencies {
    implementation("net.neoforged:neoforge:20.4.219")
    compileOnly(project(":common"))
    implementation("net.caffeinemc:fabric_api_base:0.4.32")
    jarJar("net.caffeinemc:fabric_api_base:[0.4.32,0.4.33)")
    implementation("net.caffeinemc:fabric_renderer_api_v1:3.2.1")
    jarJar("net.caffeinemc:fabric_renderer_api_v1:[3.2.1, 3.2.2)")
    implementation("net.caffeinemc:fabric_rendering_data_attachment_v1:0.3.37")
    jarJar("net.caffeinemc:fabric_rendering_data_attachment_v1:[0.3.37,0.3.38)")
    implementation("com.lodborg:interval-tree:1.0.0")
    jarJar("com.lodborg:interval-tree:[1.0.0,1.0.1)")
    implementation("net.caffeinemc:fabric_block_view_api_v2:1.0.1")
    jarJar("net.caffeinemc:fabric_block_view_api_v2:[1.0.1, 1.0.2)")

    compileOnly("maven.modrinth:immersiveengineering:11mMmtHT")
}

tasks.jarJar {
    archiveClassifier = "jarJar"
}

// NeoGradle compiles the game, but we don't want to add our common code to the game's code
val notNeoTask: (Task) -> Boolean = { it: Task -> !it.name.startsWith("neo") && !it.name.startsWith("compileService") }

tasks.withType<JavaCompile>().matching(notNeoTask).configureEach {
    source(project(":common").sourceSets.main.get().allSource)
    source(project(":common").sourceSets.getByName("api").allSource)
}

tasks.withType<Javadoc>().matching(notNeoTask).configureEach {
    source(project(":common").sourceSets.main.get().allJava)
    source(project(":common").sourceSets.getByName("api").allJava)
}

tasks.withType<ProcessResources>().matching(notNeoTask).configureEach {
    from(project(":common").sourceSets.main.get().resources)
}

java.toolchain.languageVersion = JavaLanguageVersion.of(17)

publishing {
    publications {

    }
    repositories {
        maven {
            url = uri("file://" + System.getenv("local_maven"))
        }
    }
}