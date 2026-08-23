plugins {
    id("io.micronaut.build.internal.jakarta-el-test-suite")
    id("org.jetbrains.kotlin.jvm")
    id("io.micronaut.build.internal.kotlin-base")
    id("io.micronaut.build.internal.kotlin-ksp")
}

dependencies {
    kspTest(projects.micronautJakartaElProcessor)
    kspTest(mn.micronaut.inject.kotlin)

    testImplementation(platform(libs.micronaut.core))
    testImplementation(projects.micronautJakartaEl)
    testImplementation(mn.micronaut.core)
    testImplementation(mn.micronaut.inject)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
