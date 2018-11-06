import org.jetbrains.kotlin.gradle.dsl.Coroutines
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.application

val kotlinVersion = "1.3.0"
val ktorVersion = "1.0.0-beta-3"
val mssqlJdbcVersion = "7.0.0.jre8"
val hikariCpVersion = "3.1.0"

// apply gradle plugins
plugins {
    java
    application
    kotlin("jvm") version "1.3.0"
}

application {
    mainClassName = "ro.minifarm.bizpharma.stockwebservice.MainKt"
}

// project details
group = "ro.minifarm.bizpharma"
version = "0.1"

// project repositories
repositories {
    mavenCentral()
    jcenter()
    // ktor
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
    // kotlin-eap (kotlin 1.3.0)
    maven { url = uri("https://dl.bintray.com/kotlin/kotlin-eap") }
    // gradle plugins repository
    maven { url = uri("https://plugins.gradle.org/m2/") }
}

// project dependencies
dependencies {
    compile(kotlin("stdlib-jdk8"))

    compile("ch.qos.logback:logback-classic:1.2.3")
    compile("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.0.0")
    // gson is faster with many small objects/files
    // compile("io.ktor:ktor-gson:$ktorVersion")
    // jackson is faster with larger objects/files
    compile("io.ktor:ktor-jackson:$ktorVersion")
    compile("com.microsoft.sqlserver:mssql-jdbc:$mssqlJdbcVersion")
    compile("com.zaxxer:HikariCP:$hikariCpVersion")
    compile("io.ktor:ktor-server-netty:$ktorVersion")
    compile("io.ktor:ktor-auth-jwt:$ktorVersion")

    testCompile("junit", "junit", "4.12")
}

// kotlin configuration:
configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}