package org.mariotaku.qiniupublisherplugin

import org.gradle.api.internal.model.DefaultObjectFactory

open class QiniuPublisherExtensions(factory: DefaultObjectFactory) {

    var accessKey: String = ""
    var secretKey: String = ""
    var bucket: String = ""

    var overrideKey: String? = null
    var overrideMappingKey: String? = null
    var keyPrefix: String? = null
    var keySuffix: String? = null

    var uploadMapping: Boolean = false
    var refreshCdn: Boolean = false
    var refreshCdnUrl: Set<String>? = null
}
