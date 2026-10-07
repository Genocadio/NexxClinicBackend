package com.nexxserve.nexxclinic.service;

import com.nexxserve.nexxclinic.config.SupabaseProperties;
import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.messages.Item;
import io.minio.http.Method;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

@Service
@ConditionalOnProperty(name = "supabase.storage-type", havingValue = "MINIO")
public class MinioStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageService.class);

    private static final String PUBLIC_BUCKET_POLICY_TEMPLATE = """
        {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},
        "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}
        """;

    private final SupabaseProperties properties;
    private final MinioClient client;
    private final MinioClient presignClient;
    private final Set<String> publicPoliciesConfigured = new HashSet<>();

    @Autowired
    public MinioStorageService(SupabaseProperties properties) {
        this.properties = properties;
        this.client = createClient(properties.getMinioEndpoint(), properties);
        String publicEndpoint = hasText(properties.getMinioPublicEndpoint())
            ? properties.getMinioPublicEndpoint()
            : properties.getMinioEndpoint();
        this.presignClient = createClient(publicEndpoint, properties);
    }

    MinioStorageService(SupabaseProperties properties, MinioClient client, MinioClient presignClient) {
        this.properties = properties;
        this.client = client;
        this.presignClient = presignClient;
    }

    private MinioClient createClient(String endpoint, SupabaseProperties props) {
        if (!hasText(endpoint)) {
            throw new IllegalStateException("MINIO_ENDPOINT is required when STORAGE_TYPE=MINIO");
        }
        if (!hasText(props.getMinioAccessKey()) || !hasText(props.getMinioSecretKey())) {
            throw new IllegalStateException(
                "MINIO_ACCESS_KEY and MINIO_SECRET_KEY are required when STORAGE_TYPE=MINIO"
            );
        }
        return MinioClient.builder()
            .endpoint(endpoint)
            .credentials(props.getMinioAccessKey(), props.getMinioSecretKey())
            .region(props.getMinioRegion())
            .build();
    }

    @Override
    public void upload(byte[] data, String bucket, String objectPath, String contentType)
            throws IOException {
        validateObjectKey(objectPath);
        try {
            ensureBucket(bucket);
            client.putObject(PutObjectArgs.builder()
                .bucket(bucket)
                .object(objectPath)
                .stream(new ByteArrayInputStream(data), data.length, -1)
                .contentType(contentType)
                .build());
        } catch (Exception e) {
            throw storageFailure("upload", bucket, objectPath, e);
        }
    }

    @Override
    public byte[] download(String bucket, String objectPath) throws IOException {
        validateObjectKey(objectPath);
        try {
            ensureBucket(bucket);
            try (InputStream input = client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectPath)
                    .build())) {
                return input.readAllBytes();
            }
        } catch (Exception e) {
            throw storageFailure("download", bucket, objectPath, e);
        }
    }

    @Override
    public void ensureDirectory(String bucket, String directoryPath) throws IOException {
        String prefix = trimSlashes(directoryPath);
        if (prefix.isBlank()) {
            return;
        }
        upload(new byte[0], bucket, prefix + "/.keep", "application/octet-stream");
    }

    @Override
    public void movePrefix(String bucket, String sourcePrefix, String destinationPrefix)
            throws IOException {
        String source = trimSlashes(sourcePrefix);
        String destination = trimSlashes(destinationPrefix);
        validateObjectKey(source);
        validateObjectKey(destination);
        if (source.equals(destination)) {
            return;
        }
        if (destination.startsWith(source + "/")) {
            throw new IOException("Cannot move a storage prefix inside itself");
        }

        try {
            ensureBucket(bucket);
            List<String> objects = listObjects(bucket, source);
            for (String sourceObject : objects) {
                String suffix = sourceObject.substring(source.length());
                String destinationObject = destination + suffix;
                client.copyObject(CopyObjectArgs.builder()
                    .bucket(bucket)
                    .object(destinationObject)
                    .source(CopySource.builder().bucket(bucket).object(sourceObject).build())
                    .build());
            }
            for (String sourceObject : objects) {
                client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(sourceObject)
                    .build());
            }
            log.info("Moved MinIO storage prefix {}/{} to {}", bucket, source, destination);
        } catch (Exception e) {
            throw storageFailure("move prefix", bucket, source, e);
        }
    }

    private List<String> listObjects(String bucket, String prefix) throws Exception {
        List<String> objects = new ArrayList<>();
        Iterable<io.minio.Result<Item>> results = client.listObjects(ListObjectsArgs.builder()
            .bucket(bucket)
            .prefix(prefix + "/")
            .recursive(true)
            .build());
        for (io.minio.Result<Item> result : results) {
            objects.add(result.get().objectName());
        }
        return objects;
    }

    @Override
    public void delete(String bucket, String objectPath) throws IOException {
        validateObjectKey(objectPath);
        try {
            ensureBucket(bucket);
            client.removeObject(RemoveObjectArgs.builder()
                .bucket(bucket)
                .object(objectPath)
                .build());
        } catch (Exception e) {
            throw storageFailure("delete", bucket, objectPath, e);
        }
    }

    @Override
    public String getPublicUrl(String bucket, String objectPath) {
        String endpoint = hasText(properties.getMinioPublicEndpoint())
            ? properties.getMinioPublicEndpoint()
            : properties.getMinioEndpoint();
        return endpoint.replaceAll("/+$", "") + "/" + bucket + "/"
            + UriUtils.encodePath(objectPath, java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    public String getSignedUrl(String bucket, String objectPath, int expiresInSeconds)
            throws IOException {
        validateObjectKey(objectPath);
        if (expiresInSeconds < 1 || expiresInSeconds > 7 * 24 * 60 * 60) {
            throw new IOException("MinIO signed URL expiry must be between 1 second and 7 days");
        }
        try {
            ensureBucket(bucket);
            return presignClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectPath)
                .expiry(expiresInSeconds)
                .build());
        } catch (Exception e) {
            throw storageFailure("create signed URL", bucket, objectPath, e);
        }
    }

    @Override
    public String getStoragePath(String bucket, String objectPath) {
        return objectPath;
    }

    private synchronized void ensureBucket(String bucket) throws Exception {
        if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            client.makeBucket(MakeBucketArgs.builder()
                .bucket(bucket)
                .region(properties.getMinioRegion())
                .build());
        }
        if (bucket.equals(properties.getBucketPublic()) && publicPoliciesConfigured.add(bucket)) {
            try {
                client.setBucketPolicy(SetBucketPolicyArgs.builder()
                    .bucket(bucket)
                    .config(PUBLIC_BUCKET_POLICY_TEMPLATE.formatted(bucket))
                    .build());
            } catch (Exception e) {
                publicPoliciesConfigured.remove(bucket);
                throw e;
            }
        }
    }

    private void validateObjectKey(String objectKey) throws IOException {
        if (!hasText(objectKey) || objectKey.startsWith("/") || objectKey.contains("\\")
                || List.of(objectKey.split("/", -1)).contains("..")
                || List.of(objectKey.split("/", -1)).contains(".")) {
            throw new IOException("Invalid storage object key");
        }
    }

    private String trimSlashes(String value) {
        if (value == null) {
            return "";
        }
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '/') start++;
        while (end > start && value.charAt(end - 1) == '/') end--;
        return value.substring(start, end);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private IOException storageFailure(String operation, String bucket, String key, Exception cause) {
        log.error("MinIO {} failed for {}/{}: {}", operation, bucket, key, cause.getMessage(), cause);
        return new IOException("MinIO " + operation + " failed for bucket " + bucket, cause);
    }
}
