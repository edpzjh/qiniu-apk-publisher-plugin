package org.mariotaku.qiniupublisherplugin

import com.android.build.gradle.AndroidConfig
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.mariotaku.qiniupublisherplugin.model.FlavorScope
import java.io.File

class QiniuPublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        if (!project.hasProperty("android")) {
            throw IllegalArgumentException("Project ${project.name} is not an Android project")
        }
        val config = project.extensions.create("qiniuPublish",
                QiniuPublisherExtensions::class.java, project.objects)
        project.afterEvaluate {
            setupTasks(it, config)
        }
    }

    private fun setupTasks(project: Project, config: QiniuPublisherExtensions) {
        val android = project.property("android") as AndroidConfig
        val buildTypeNames = android.buildTypes.map { type -> type.name }
        val buildVariants = FlavorScope.list(android)

        buildTypeNames.forEach { buildTypeName ->
            val typeTasks = buildVariants.map { buildVariant ->
                val targetName = buildVariant.camelCaseName(buildTypeName)

                val apkName = "${buildVariant.snakeCaseName(buildTypeName, project.name)}.apk"
                val apkPath = arrayOf(project.buildDir, "outputs", "apk", buildVariant.camelCaseName,
                        buildTypeName, apkName).joinToString(File.separator)
                val mappingPath = arrayOf(project.buildDir, "outputs", "mapping", buildVariant.camelCaseName,
                        buildTypeName, "mapping.txt").joinToString(File.separator)

                // Bundle task name for variant
                val qiniuPublishTaskName = buildVariant.camelCaseName(buildTypeName, "qiniuPublish")
                val assembleTaskName = buildVariant.camelCaseName(buildTypeName, "assemble")

                return@map project.tasks.create(qiniuPublishTaskName, QiniuPublishTask::class.java) {
                    it.group = "qiniu-publish"
                    it.description = "Publish $targetName apk to Qiniu."
                    it.config = config
                    it.apkFile = File(apkPath)
                    it.mappingFile = File(mappingPath)

                    it.dependsOn(assembleTaskName)
                }

            }
            if (!FlavorScope.noVariants(buildVariants)) {
                project.tasks.create("qiniuPublish${buildTypeName.capitalize()}") {
                    it.group = "qiniu-publish"
                    it.dependsOn(typeTasks)
                }
            }
        }
    }

}
