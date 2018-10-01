package org.mariotaku.qiniupublisherplugin

import com.android.build.gradle.api.BaseVariantOutput
import com.qiniu.cdn.CdnManager
import com.qiniu.storage.Configuration
import com.qiniu.storage.UploadManager
import com.qiniu.util.Auth
import org.gradle.api.Plugin
import org.gradle.api.Project

class QiniuPublishPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        if (!project.hasProperty("android")) {
            throw IllegalArgumentException("Project ${project.name} is not an Android project")
        }
        def config = project.extensions.create("qiniuPublish", QiniuPublisherExtensions)
        project.afterEvaluate { p ->
            p.android.applicationVariants.forEach { variant ->
                BaseVariantOutput output = variant.outputs.first()

                // Bundle task name for variant
                def qiniuPublishTaskName = "qiniuPublish${variant.name.capitalize()}"

                p.task(qiniuPublishTaskName) {
                    group = "qiniu-publish"
                    description = "Publish ${variant.name} apk to Qiniu."

                    doLast {
                        putObject(config, output.outputFile, apkKey(config, output.outputFile))

                        if (config.uploadMapping && mappingFile.exists()) {
                            putObject(config, variant.mappingFile, mappingKey(config, output.outputFile))
                        }
                    }

                    dependsOn(variant.assemble)
                }
            }
        }
    }


    static String apkKey(QiniuPublisherExtensions config, File file) {
        def uploadName = config.overrideKey
        if (uploadName != null) return uploadName
        def prefix = config.keyPrefix.orEmpty()
        def suffix = config.keySuffix.orEmpty()
        return "$prefix${nameWithoutExtension(file)}$suffix.${extension(file)}"
    }

    static String mappingKey(QiniuPublisherExtensions config, File file) {
        def uploadName = config.overrideMappingKey
        if (uploadName != null) return uploadName
        def prefix = config.keyPrefix.orEmpty()
        def suffix = config.keySuffix.orEmpty()
        return "${prefix}mapping-${nameWithoutExtension(file)}$uploadName$suffix.txt"
    }

    static String mediaType(File file) {
        switch (extension(file).toLowerCase(Locale.US)) {
            case "apk": return "application/vnd.android.package-archive"
            case "txt": return "text/plain"
            default: return "application/octet-stream"
        }
    }

    static String extension(File file) {
        def index = file.name.lastIndexOf('.')
        if (index < 0) return ''
        return file.name.substring(index + 1)
    }

    static String nameWithoutExtension(File file) {
        def index = file.name.lastIndexOf('.')
        if (index < 0) return file.name
        return file.name.substring(0, index)
    }

    static void putObject(QiniuPublisherExtensions config, File file, String key) {
        def auth = Auth.create(config.accessKey, config.secretKey)
        def upToken = auth.uploadToken(config.bucket, key)
        def uploadManager = new UploadManager(new Configuration())
        def response = uploadManager.put(file, key, upToken, null, mediaType(file),
                true)
        if (!response.OK) throw new IOException("HTTP ${response.statusCode}: ${response.error}")

        def refreshCdnUrl = config.refreshCdnUrls
        if (config.refreshCdn && refreshCdnUrl) {
            def cdnManager = new CdnManager(auth)
            cdnManager.refreshUrls(refreshCdnUrl.toArray(new String[refreshCdnUrl.size()]))
        }
    }

}
