plugins {
    kotlin("js") version "1.3.72"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://kotlin.bintray.com/kotlin-js-wrappers/")
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-js"))
    testImplementation(kotlin("test-js"))

    implementation("org.jetbrains:kotlin-react:16.13.1-pre.105-kotlin-1.3.72")
    implementation("org.jetbrains:kotlin-react-dom:16.13.1-pre.105-kotlin-1.3.72")
    implementation(npm("react", "16.13.1"))
    implementation(npm("react-dom", "16.13.1"))

    implementation("org.jetbrains:kotlin-styled:1.0.0-pre.105-kotlin-1.3.72")

    implementation(npm("styled-components"))
    implementation(npm("inline-style-prefixer"))
}

kotlin.target.browser { }