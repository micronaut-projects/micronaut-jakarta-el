plugins {
    id("io.micronaut.build.internal.jakarta-el-test-suite")
    id("org.jetbrains.kotlin.jvm")
    id("io.micronaut.build.internal.kotlin-base")
    id("io.micronaut.build.internal.kotlin-ksp")
}

// The examples of the guide, in Kotlin, processed with KSP.
dependencies {
    kspTest(projects.micronautJakartaElProcessor)
    kspTest(mn.micronaut.inject.kotlin)
    kspTest(projects.testSuiteCustomAnnotation)

    testImplementation(platform(libs.micronaut.core))
    testImplementation(projects.micronautJakartaEl)
    testImplementation(projects.testSuiteCustomAnnotation)
    testImplementation(mn.micronaut.core)
    testImplementation(mn.micronaut.inject)
    testImplementation(mn.micronaut.aop)
    testRuntimeOnly(mn.micronaut.context)
    testRuntimeOnly(projects.micronautJakartaElInterpreter)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.jupiter.engine)
}
