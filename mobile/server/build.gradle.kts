plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    kotlin("plugin.serialization") version "2.3.0"
    application
}

group = "com.gembotics.ctv"
version = "1.0.0"
application {
    mainClass.set("com.gembotics.ctv.ApplicationKt")
    
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    implementation(libs.ktor.serverCors)
    implementation(libs.kotlinx.serializationJson)
    implementation(libs.bitcoinj)
    // BouncyCastle for RIPEMD160 support (workaround for bitcoinj hash160 bug)
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}