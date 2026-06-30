plugins {
    kotlin("jvm") version "1.9.20"
    id("com.google.protobuf") version "0.9.6"
    application
}

group = "org.freeplane.grpc"
version = "0.1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-guava:1.7.3")
    implementation("io.grpc:grpc-protobuf:1.52.0")
    implementation("io.grpc:grpc-stub:1.52.0")
    implementation("io.grpc:grpc-netty-shaded:1.52.0")
    implementation("com.google.protobuf:protobuf-kotlin:3.21.12")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.20")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.grpc:grpc-testing:1.52.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-core:5.5.0")
}

application {
    mainClass.set("org.freeplane.grpc.examples.BasicUsageKt")
}

sourceSets {
    main {
        kotlin {
            srcDirs("src/main/kotlin", "examples")
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.21.12"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.52.0"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.plugins {
                create("grpc") { }
            }
        }
    }
}

kotlin {
    jvmToolchain(21)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
