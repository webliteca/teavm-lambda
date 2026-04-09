package ca.weblite.teavmlambda.impl.jvm.s3;

import ca.weblite.teavmlambda.api.objectstore.ObjectStoreClient;
import ca.weblite.teavmlambda.api.objectstore.ObjectStoreProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;

/**
 * JVM-based ObjectStore provider for Amazon S3.
 * Discovered via ServiceLoader.
 *
 * <p>Connection URI formats:</p>
 * <ul>
 *   <li>{@code s3://us-east-1} - S3 in us-east-1</li>
 *   <li>{@code s3://localhost:9000} - MinIO / S3-compatible local endpoint (HTTP)</li>
 *   <li>{@code s3://account.r2.cloudflarestorage.com} - S3-compatible HTTPS endpoint (e.g. CloudFlare R2)</li>
 * </ul>
 */
public class AwsS3ObjectStoreProvider implements ObjectStoreProvider {

    @Override
    public String getScheme() {
        return "s3";
    }

    @Override
    public ObjectStoreClient create(String uri) {
        String remainder = uri.substring("s3://".length());

        S3ClientBuilder builder = S3Client.builder();

        if (remainder.contains(":")) {
            builder.endpointOverride(URI.create("http://" + remainder))
                    .region(Region.US_EAST_1)
                    .forcePathStyle(true);
        } else if (remainder.contains(".")) {
            builder.endpointOverride(URI.create("https://" + remainder))
                    .region(Region.of("auto"))
                    .forcePathStyle(true);
        } else {
            builder.region(Region.of(remainder));
        }

        return new AwsS3ObjectStoreClient(builder.build());
    }
}
