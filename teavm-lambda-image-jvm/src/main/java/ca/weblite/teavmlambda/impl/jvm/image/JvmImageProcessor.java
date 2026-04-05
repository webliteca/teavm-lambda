package ca.weblite.teavmlambda.impl.jvm.image;

import ca.weblite.teavmlambda.api.image.ImageFormat;
import ca.weblite.teavmlambda.api.image.ImageOptions;
import ca.weblite.teavmlambda.api.image.ImageProcessor;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

/**
 * JVM implementation of {@link ImageProcessor} using javax.imageio.ImageIO.
 */
public class JvmImageProcessor implements ImageProcessor {

    @Override
    public byte[] process(byte[] input, ImageOptions options) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(input));
            if (image == null) {
                throw new RuntimeException("Unable to decode image: unsupported format");
            }

            if (options.getWidth() != null || options.getHeight() != null) {
                image = resize(image, options);
            }

            ImageFormat format = options.getFormat();
            String formatName = format != null ? toFormatName(format) : detectFormat(input);

            return writeImage(image, formatName, options.getQuality());
        } catch (IOException e) {
            throw new RuntimeException("Image processing failed: " + e.getMessage(), e);
        }
    }

    private BufferedImage resize(BufferedImage source, ImageOptions options) {
        int targetWidth = options.getWidth() != null ? options.getWidth() : 0;
        int targetHeight = options.getHeight() != null ? options.getHeight() : 0;

        if (options.isPreserveAspectRatio()) {
            double widthRatio = targetWidth > 0 ? (double) targetWidth / source.getWidth() : Double.MAX_VALUE;
            double heightRatio = targetHeight > 0 ? (double) targetHeight / source.getHeight() : Double.MAX_VALUE;
            double ratio = Math.min(widthRatio, heightRatio);
            if (ratio == Double.MAX_VALUE) {
                ratio = Math.min(widthRatio, heightRatio);
            }
            targetWidth = (int) Math.round(source.getWidth() * ratio);
            targetHeight = (int) Math.round(source.getHeight() * ratio);
        } else {
            if (targetWidth <= 0) targetWidth = source.getWidth();
            if (targetHeight <= 0) targetHeight = source.getHeight();
        }

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, getImageType(source));
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        g.dispose();
        return resized;
    }

    private int getImageType(BufferedImage source) {
        return source.getType() != 0 ? source.getType() : BufferedImage.TYPE_INT_ARGB;
    }

    private byte[] writeImage(BufferedImage image, String formatName, Integer quality) throws IOException {
        // For JPEG, ensure no alpha channel
        if ("jpeg".equalsIgnoreCase(formatName) || "jpg".equalsIgnoreCase(formatName)) {
            image = removeAlpha(image);
        }

        if (quality != null && ("jpeg".equalsIgnoreCase(formatName) || "jpg".equalsIgnoreCase(formatName)
                || "webp".equalsIgnoreCase(formatName))) {
            return writeWithQuality(image, formatName, quality);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        if (!ImageIO.write(image, formatName, baos)) {
            throw new RuntimeException("No ImageIO writer found for format: " + formatName);
        }
        return baos.toByteArray();
    }

    private byte[] writeWithQuality(BufferedImage image, String formatName, int quality) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            throw new RuntimeException("No ImageIO writer found for format: " + formatName);
        }
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed()) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            String[] types = param.getCompressionTypes();
            if (types != null && types.length > 0) {
                param.setCompressionType(types[0]);
            }
            param.setCompressionQuality(quality / 100.0f);
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (var stream = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private BufferedImage removeAlpha(BufferedImage image) {
        if (!image.getColorModel().hasAlpha()) {
            return image;
        }
        BufferedImage rgb = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, image.getWidth(), image.getHeight());
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static String toFormatName(ImageFormat format) {
        switch (format) {
            case PNG: return "png";
            case JPEG: return "jpeg";
            case WEBP: return "webp";
            default: throw new IllegalArgumentException("Unsupported format: " + format);
        }
    }

    private static String detectFormat(byte[] data) {
        if (data.length >= 8 && data[0] == (byte) 0x89 && data[1] == (byte) 0x50
                && data[2] == (byte) 0x4E && data[3] == (byte) 0x47) {
            return "png";
        }
        if (data.length >= 2 && data[0] == (byte) 0xFF && data[1] == (byte) 0xD8) {
            return "jpeg";
        }
        if (data.length >= 4 && data[0] == (byte) 0x52 && data[1] == (byte) 0x49
                && data[2] == (byte) 0x46 && data[3] == (byte) 0x46) {
            return "webp";
        }
        return "png"; // default fallback
    }
}
