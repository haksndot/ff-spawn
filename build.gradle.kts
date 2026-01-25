plugins {
    java
}

group = "com.haksndot"
version = "1.0.0"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io") // For GriefPrevention
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    compileOnly("com.github.TechFortress:GriefPrevention:16.18.4")
}

tasks.jar {
    archiveFileName.set("DonutSpawn-${version}.jar")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
