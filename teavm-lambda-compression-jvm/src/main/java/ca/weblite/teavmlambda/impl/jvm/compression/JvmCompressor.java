package ca.weblite.teavmlambda.impl.jvm.compression;

import ca.weblite.teavmlambda.api.compression.Compressor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

/**
 * JVM-based compression using {@code java.util.zip}.
 */
public final class JvmCompressor implements Compressor {

    @Override
    public String gzip(String data) {
        try {
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
            try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
                gzos.write(input);
            }
            return new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new RuntimeException("gzip compression failed", e);
        }
    }

    @Override
    public String deflate(String data) {
        try {
            byte[] input = data.getBytes(StandardCharsets.UTF_8);
            ByteArrayOutputStream baos = new ByteArrayOutputStream(input.length);
            try (DeflaterOutputStream dos = new DeflaterOutputStream(baos)) {
                dos.write(input);
            }
            return new String(baos.toByteArray(), StandardCharsets.ISO_8859_1);
        } catch (IOException e) {
            throw new RuntimeException("deflate compression failed", e);
        }
    }
}
