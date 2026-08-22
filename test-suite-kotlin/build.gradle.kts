plugins {
    id("io.micronaut.build.internal.expression-language-test-suite")
    id("org.jetbrains.kotlin.jvm")
    id("io.micronaut.build.internal.kotlin-base")
    id("io.micronaut.build.internal.kotlin-ksp")
}

dependencies {
    kspTest(projects.micronautExpressionLanguageProcessor)
    kspTest(mn.micronaut.inject.kotlin)

    testImplementation(platform(libs.micronaut.core))
    testImplementation(projects.micronautExpressionLanguage)
    testImplementation(mn.micronaut.core)
    testImplementation(mn.micronaut.inject)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
