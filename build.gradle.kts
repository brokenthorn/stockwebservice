import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val ktorVersion = "1.0.0-beta-3"
val mssqlJdbcVersion = "7.1.2.jre11-preview"

plugins {
    java
    kotlin("jvm") version "1.3.0"
}

group = "ro.minifarm.bizpharma"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    jcenter()
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile("ch.qos.logback:logback-classic:1.2.3")

    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")

    //compile("io.ktor:ktor-gson:$ktorVersion")
    compile("io.ktor:ktor-jackson:$ktorVersion")

    compile("com.microsoft.sqlserver:mssql-jdbc:$mssqlJdbcVersion")

    compile("com.zaxxer:HikariCP:3.1.0")

    compile("io.ktor:ktor-server-netty:$ktorVersion")

    compile("io.ktor:ktor-auth-jwt:$ktorVersion")

    testCompile("junit", "junit", "4.12")
}


configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}