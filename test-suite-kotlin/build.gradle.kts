plugins {
    id("io.micronaut.build.internal.expression-language-test-suite")
    id("org.jetbrains.kotlin.jvm")
    id("io.micronaut.build.internal.kotlin-base")
    id("io.micronaut.build.internal.kotlin-ksp")
}

// The annotation processor is deliberately absent: the Kotlin source writer of Micronaut SourceGen does not yet
// emit the generated expressions, so an expression declared in Kotlin source is evaluated by the interpreter.
// See the "Language support" section of the README.
dependencies {
    kspTest(mn.micronaut.inject.kotlin)

    testImplementation(platform(libs.micronaut.core))
    testImplementation(projects.micronautExpressionLanguage)
    testImplementation(projects.micronautExpressionLanguageInterpreter)
    testImplementation(mn.micronaut.core)
    testImplementation(mn.micronaut.inject)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
