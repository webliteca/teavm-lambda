package ca.weblite.teavmlambda.impl.jvm.s3;

import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * JVM implementation of ObjectStoreClient using the AWS SDK for Java v2.
 */
public class AwsS3ObjectStoreClient implements ObjectStoreClient {

    private final S3Client s3;

    AwsS3ObjectStoreClient(S3Client s3) {
        this.s3 = s3;
    }

    @Override
    public void putObject(String bucket, String key, String data, String contentType) {
        s3.putObject(
                PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromString(data));
    }

    @Override
    public String getObject(String bucket, String key) {
        try {
            ResponseBytes<GetObjectResponse> response = s3.getObjectAsBytes(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build());
            return response.asString(StandardCharsets.UTF_8);
        } catch (NoSuchKeyException e) {
            return null;
        }
    }

    @Override
    public void deleteObject(String bucket, String key) {
        s3.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .build());
    }

    @Override
    public List<String> listObjects(String bucket, String prefix) {
        ListObjectsV2Request.Builder builder = ListObjectsV2Request.builder().bucket(bucket);
        if (prefix != null && !prefix.isEmpty()) {
            builder.prefix(prefix);
        }
        ListObjectsV2Response response = s3.listObjectsV2(builder.build());
        List<String> keys = new ArrayList<>();
        for (S3Object obj : response.contents()) {
            keys.add(obj.key());
        }
        return keys;
    }

    @Override
    public boolean objectExists(String bucket, String key) {
        try {
            s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }
}
