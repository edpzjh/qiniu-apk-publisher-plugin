package org.mariotaku.qiniupublisherplugin

class QiniuPublisherExtensions {

    String accessKey = ""
    String secretKey = ""
    String bucket = ""

    String overrideKey = null
    String overrideMappingKey = null
    String keyPrefix = null
    String keySuffix = null

    boolean uploadMapping = false
    boolean refreshCdn = false
    Set<String> refreshCdnUrls = null
}
