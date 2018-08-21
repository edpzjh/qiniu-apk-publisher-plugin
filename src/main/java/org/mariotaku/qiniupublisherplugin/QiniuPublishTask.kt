package org.mariotaku.qiniupublisherplugin

import com.qiniu.cdn.CdnManager
import com.qiniu.storage.Configuration
import com.qiniu.storage.UploadManager
import com.qiniu.util.Auth
import org.gradle.api.DefaultTask
import java.io.File
import java.io.IOException
import java.util.*


open class QiniuPublishTask : DefaultTask() {

    lateinit var config: QiniuPublisherExtensions
    lateinit var apkFile: File
    lateinit var mappingFile: File

    init {
        doLast {
            putObject(config.bucket, apkFile.apkKey, apkFile)

            if (config.uploadMapping && mappingFile.exists()) {
                putObject(config.bucket, apkFile.mappingKey, mappingFile)
            }
        }
    }

    private val File.apkKey: String
        get() {
            val uploadName = config.overrideKey
            if (uploadName != null) return uploadName
            val prefix = config.keyPrefix.orEmpty()
            val suffix = config.keySuffix.orEmpty()
            return "$prefix$nameWithoutExtension$suffix.$extension"
        }

    private val File.mappingKey: String
        get() {
            val uploadName = config.overrideMappingKey
            if (uploadName != null) return uploadName
            val prefix = config.keyPrefix.orEmpty()
            val suffix = config.keySuffix.orEmpty()
            return "${prefix}mapping-$nameWithoutExtension$uploadName$suffix.txt"
        }

    private val File.mediaType: String
        get() = when (extension.toLowerCase(Locale.US)) {
            "apk" -> "application/vnd.android.package-archive"
            "txt" -> "text/plain"
            else -> "application/octet-stream"
        }

    private fun putObject(bucket: String, key: String, file: File) {
        val auth = Auth.create(config.accessKey, config.secretKey)
        val upToken = auth.uploadToken(bucket, key)
        val uploadManager = UploadManager(Configuration())
        val response = uploadManager.put(file, key, upToken, null, file.mediaType,
                true)
        if (!response.isOK) throw IOException("HTTP ${response.statusCode}: ${response.error}")

        val refreshCdnUrl = config.refreshCdnUrls
        if (config.refreshCdn && refreshCdnUrl != null) {
            val cdnManager = CdnManager(auth)
            cdnManager.refreshUrls(refreshCdnUrl.toTypedArray())
        }
    }


}
