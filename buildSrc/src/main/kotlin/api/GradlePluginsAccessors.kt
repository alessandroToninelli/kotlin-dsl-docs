package api

import java.io.ByteArrayOutputStream
import java.io.File

import org.gradle.api.*
import org.gradle.api.provider.*
import org.gradle.api.tasks.*

open class GradlePluginsAccessors : DefaultTask() {

    @get:InputDirectory
    var gradleInstall: File? = null

    @get:Internal
    var buildDirectory: File? = null

    @get:InputFile
    val buildScript: File
        get() = File(buildDirectory!!, "build.gradle.kts")

    private
    val accessorsDirState = project.property(File::class.java)

    @get:OutputDirectory
    var accessorsDir
        get() = accessorsDirState.get()
        set(value) = accessorsDirState.set(value)

    @get:Internal
    val accessorsDirProvider: Provider<File>
        get() = accessorsDirState

    @TaskAction
    fun gradlePluginsAccessors() {
        accessorsDir.deleteRecursively()
        val baos = ByteArrayOutputStream()
        val gradlew = File(gradleInstall!!, "bin/gradle")
        project.exec {
            commandLine = listOf(
                    gradlew.absolutePath, "-q",
                    "-c", File(buildDirectory!!, "settings.gradle").absolutePath,
                    "-p", buildDirectory!!.absolutePath,
                    "kotlinDslAccessorsReport")
            standardOutput = baos
        }
        val text = """
        package org.gradle.kotlin.dsl

        import org.gradle.api.*
        import org.gradle.api.artifacts.*
        import org.gradle.api.artifacts.dsl.*

        ${String(baos.toByteArray())}
        """
        val accessorsFile = File(accessorsDir, "GradlePluginsAccessors.kt")
        accessorsFile.parentFile.mkdirs()
        accessorsFile.writeText(text)
    }

}