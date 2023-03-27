import com.heroku.sdk.HerokuExtension


val ktorVersion: String = "2.2.4"
val kotlinVersion: String = "1.6.10"
val logbackVersion: String = "1.2.11"
val arrowVersion: String = "1.1.2"
val gsonVersion: String = "2.9.0"

plugins {
    application
    kotlin("jvm") version "1.7.0"
//    kotlin("plugin.serialization") version "1.7.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.7.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("com.heroku.sdk.heroku-gradle") version "3.0.0"
}

val appName = "habit-app"
val mainClassName = "com.me.postfetcher.ApplicationKt"

tasks.register("stage") {
    dependsOn("shadowJar")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set("shadow")
        mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "com.github.csolem.gradle.shadow.kotlin.example.App"))
        }
    }
}

tasks.withType<JavaCompile> {
    options.isFork = true
    options.forkOptions.jvmArgs = listOf("-Xmx2048m")
}

configure<HerokuExtension> {
    this.appName = appName
}

group = "com.me.postfetcher"
version = "0.0.1"

application {
    mainClass.set("com.me.postfetcher.ApplicationKt")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/ktor/eap") }
}

kotlin.sourceSets["test"].kotlin.srcDirs("test/kotlin")

tasks.withType<Test>().configureEach {
    maxParallelForks = 1
    setForkEvery(100)
    reports.html.required.set(true)
    reports.junitXml.required.set(true)
}

dependencies {
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-coroutines:$arrowVersion")
    implementation("io.arrow-kt:arrow-fx-stm:$arrowVersion")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("io.arrow-kt:arrow-core:$arrowVersion")
    implementation("io.mockk:mockk:1.12.4")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    implementation("org.jetbrains.exposed:exposed-core:0.41.1")
    implementation("org.jetbrains.exposed:exposed-dao:0.41.1")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.41.1")
    implementation("org.postgresql:postgresql:42.2.24")

    implementation("com.squareup.retrofit2:retrofit:$gsonVersion")
    implementation("com.squareup.retrofit2:adapter-rxjava2:$gsonVersion")
    implementation("com.squareup.retrofit2:converter-gson:$gsonVersion")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("com.squareup.okhttp3:okhttp:3.8.1")
    implementation("io.github.microutils:kotlin-logging:2.0.11")
    implementation("com.github.tomakehurst:wiremock-jre8:2.33.2")
    implementation("io.github.microutils:kotlin-logging:2.0.11")
    implementation("org.flywaydb:flyway-core:8.5.2")
    implementation("com.h2database:h2:2.1.214")
    implementation("org.jetbrains.exposed:exposed-java-time:0.32.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")


    compileOnly("com.squareup.moshi:moshi:1.13.0")
    compileOnly("com.squareup.moshi:moshi-kotlin:1.13.0")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.2")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.6.1")

    testImplementation("io.ktor:ktor-server-tests:$ktorVersion")
    testImplementation("io.ktor:ktor-client-serialization:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-http:$ktorVersion")


//    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:$kotlinVersion")
    testImplementation("io.kotest:kotest-assertions-json:5.3.1")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.3.1")
    testImplementation("io.kotest:kotest-runner-junit5:5.3.1")
    testImplementation("io.kotest:kotest-property-jvm:5.3.1")
    testImplementation("io.kotest.extensions:kotest-assertions-ktor:1.0.3")
    testImplementation("io.kotest.extensions:kotest-assertions-arrow:1.3.0")
    testImplementation("io.kotest:kotest-assertions-core:5.3.1")

}


tasks.test {
    useJUnitPlatform()
}