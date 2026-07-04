package com.southstand.file.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.file")
public class FileProperties {

    private String storageType = "LOCAL";
    private String publicUrlPrefix = "/api/public/files";
    private long maxSizeBytes = 10 * 1024 * 1024L;
    private List<String> allowedExtensions = new ArrayList<>(List.of("jpg", "jpeg", "png", "webp", "gif"));
    private List<String> allowedContentTypes = new ArrayList<>(List.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    ));
    private Local local = new Local();
    private AliyunOss aliyunOss = new AliyunOss();
    private Qiniu qiniu = new Qiniu();
    private Minio minio = new Minio();

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getPublicUrlPrefix() {
        return publicUrlPrefix;
    }

    public void setPublicUrlPrefix(String publicUrlPrefix) {
        this.publicUrlPrefix = publicUrlPrefix;
    }

    public long getMaxSizeBytes() {
        return maxSizeBytes;
    }

    public void setMaxSizeBytes(long maxSizeBytes) {
        this.maxSizeBytes = maxSizeBytes;
    }

    public List<String> getAllowedExtensions() {
        return allowedExtensions;
    }

    public void setAllowedExtensions(List<String> allowedExtensions) {
        this.allowedExtensions = allowedExtensions;
    }

    public List<String> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    public void setAllowedContentTypes(List<String> allowedContentTypes) {
        this.allowedContentTypes = allowedContentTypes;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public AliyunOss getAliyunOss() {
        return aliyunOss;
    }

    public void setAliyunOss(AliyunOss aliyunOss) {
        this.aliyunOss = aliyunOss;
    }

    public Qiniu getQiniu() {
        return qiniu;
    }

    public void setQiniu(Qiniu qiniu) {
        this.qiniu = qiniu;
    }

    public Minio getMinio() {
        return minio;
    }

    public void setMinio(Minio minio) {
        this.minio = minio;
    }

    public static class Local {
        private String storageRoot = "uploads";

        public String getStorageRoot() {
            return storageRoot;
        }

        public void setStorageRoot(String storageRoot) {
            this.storageRoot = storageRoot;
        }
    }

    public static class AliyunOss {
        private String endpoint = "";
        private String bucket = "";
        private String accessKeyId = "";
        private String accessKeySecret = "";
        private String publicDomain = "";

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKeyId() { return accessKeyId; }
        public void setAccessKeyId(String accessKeyId) { this.accessKeyId = accessKeyId; }
        public String getAccessKeySecret() { return accessKeySecret; }
        public void setAccessKeySecret(String accessKeySecret) { this.accessKeySecret = accessKeySecret; }
        public String getPublicDomain() { return publicDomain; }
        public void setPublicDomain(String publicDomain) { this.publicDomain = publicDomain; }
    }

    public static class Qiniu {
        private String bucket = "";
        private String accessKey = "";
        private String secretKey = "";
        private String publicDomain = "";

        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getPublicDomain() { return publicDomain; }
        public void setPublicDomain(String publicDomain) { this.publicDomain = publicDomain; }
    }

    public static class Minio {
        private String endpoint = "";
        private String bucket = "";
        private String accessKey = "";
        private String secretKey = "";
        private String publicDomain = "";

        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public String getPublicDomain() { return publicDomain; }
        public void setPublicDomain(String publicDomain) { this.publicDomain = publicDomain; }
    }
}
