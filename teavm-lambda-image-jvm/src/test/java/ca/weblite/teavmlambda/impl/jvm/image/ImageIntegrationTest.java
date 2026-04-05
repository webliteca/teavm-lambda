package ca.weblite.teavmlambda.impl.jvm.image;

import ca.weblite.teavmlambda.api.image.ImageFormat;
import ca.weblite.teavmlambda.api.image.ImageOptions;
import ca.weblite.teavmlambda.api.image.ImageProcessor;
import ca.weblite.teavmlambda.api.image.ImageProcessorFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Integration tests for the JVM image processor.
 * Run via: java -cp ... ca.weblite.teavmlambda.impl.jvm.image.ImageIntegrationTest
 */
public class ImageIntegrationTest {

    private static int pass = 0;
    private static int fail = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Image IO JVM Integration Tests ===");
        System.out.println();

        ImageProcessor processor = ImageProcessorFactory.create();

        byte[] pngImage = createTestPng(400, 300, Color.BLUE);
        byte[] jpegImage = createTestJpeg(400, 300, Color.RED);

        // --- ServiceLoader discovery ---
        System.out.println("--- ServiceLoader ---");
        assertNotNull("ImageProcessorFactory.create() returns processor", processor);
        assertCondition("Processor is JvmImageProcessor",
                processor instanceof JvmImageProcessor);

        // --- Resize ---
        System.out.println();
        System.out.println("--- Resize ---");

        byte[] resized = processor.process(pngImage, ImageOptions.resize(200, 150));
        BufferedImage resizedImg = decode(resized);
        assertCondition("Resize PNG preserves aspect ratio (width <= 200)",
                resizedImg.getWidth() <= 200);
        assertCondition("Resize PNG preserves aspect ratio (height <= 150)",
                resizedImg.getHeight() <= 150);
        assertCondition("Resized image is smaller than original",
                resizedImg.getWidth() < 400 || resizedImg.getHeight() < 300);
        assertCondition("Resize output is valid PNG",
                isPng(resized));

        byte[] resizedExact = processor.process(pngImage,
                ImageOptions.resize(100, 50).preserveAspectRatio(false));
        BufferedImage resizedExactImg = decode(resizedExact);
        assertCondition("Resize without aspect ratio: width == 100",
                resizedExactImg.getWidth() == 100);
        assertCondition("Resize without aspect ratio: height == 50",
                resizedExactImg.getHeight() == 50);

        byte[] resizedWidth = processor.process(pngImage,
                new ImageOptions().width(200));
        BufferedImage resizedWidthImg = decode(resizedWidth);
        assertCondition("Resize width-only: width == 200",
                resizedWidthImg.getWidth() == 200);
        assertCondition("Resize width-only: height scaled proportionally",
                resizedWidthImg.getHeight() == 150);

        // --- Format Conversion ---
        System.out.println();
        System.out.println("--- Format Conversion ---");

        byte[] pngToJpeg = processor.process(pngImage, ImageOptions.ofFormat(ImageFormat.JPEG));
        assertCondition("PNG -> JPEG conversion produces JPEG",
                isJpeg(pngToJpeg));
        assertCondition("PNG -> JPEG output is non-empty",
                pngToJpeg.length > 0);

        byte[] jpegToPng = processor.process(jpegImage, ImageOptions.ofFormat(ImageFormat.PNG));
        assertCondition("JPEG -> PNG conversion produces PNG",
                isPng(jpegToPng));

        // --- Compression / Quality ---
        System.out.println();
        System.out.println("--- Compression ---");

        byte[] highQuality = processor.process(pngImage,
                ImageOptions.ofFormat(ImageFormat.JPEG).quality(95));
        byte[] lowQuality = processor.process(pngImage,
                ImageOptions.ofFormat(ImageFormat.JPEG).quality(10));
        assertCondition("JPEG quality 95 produces valid JPEG",
                isJpeg(highQuality));
        assertCondition("JPEG quality 10 produces valid JPEG",
                isJpeg(lowQuality));
        assertCondition("Lower quality JPEG is smaller than higher quality",
                lowQuality.length < highQuality.length);

        // --- Resize + Convert combined ---
        System.out.println();
        System.out.println("--- Resize + Convert ---");

        byte[] resizedConverted = processor.process(pngImage,
                ImageOptions.resize(100, 100).format(ImageFormat.JPEG).quality(80));
        BufferedImage rcImg = decode(resizedConverted);
        assertCondition("Resize+Convert produces JPEG",
                isJpeg(resizedConverted));
        assertCondition("Resize+Convert: dimensions reduced",
                rcImg.getWidth() <= 100 && rcImg.getHeight() <= 100);

        // --- No-op (no options set) ---
        System.out.println();
        System.out.println("--- Pass-through ---");

        byte[] passthrough = processor.process(pngImage, new ImageOptions());
        assertCondition("No options: output is valid PNG",
                isPng(passthrough));
        assertCondition("No options: output is non-empty",
                passthrough.length > 0);

        // --- Error handling ---
        System.out.println();
        System.out.println("--- Error Handling ---");

        boolean threwOnBadInput = false;
        try {
            processor.process(new byte[]{0, 1, 2, 3}, ImageOptions.resize(100, 100));
        } catch (RuntimeException e) {
            threwOnBadInput = true;
        }
        assertCondition("Invalid image data throws RuntimeException", threwOnBadInput);

        // --- Results ---
        System.out.println();
        System.out.println("=======================================");
        System.out.println("Results: " + pass + " passed, " + fail + " failed");
        System.out.println("=======================================");

        if (fail > 0) {
            System.exit(1);
        }
    }

    private static void assertNotNull(String desc, Object value) {
        if (value != null) {
            System.out.println("  PASS: " + desc);
            pass++;
        } else {
            System.out.println("  FAIL: " + desc + " - was null");
            fail++;
        }
    }

    private static void assertCondition(String desc, boolean condition) {
        if (condition) {
            System.out.println("  PASS: " + desc);
            pass++;
        } else {
            System.out.println("  FAIL: " + desc);
            fail++;
        }
    }

    private static byte[] createTestPng(int width, int height, Color color) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        // Add a gradient to make the image non-trivial
        g.setColor(Color.WHITE);
        g.fillOval(width / 4, height / 4, width / 2, height / 2);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }

    private static byte[] createTestJpeg(int width, int height, Color color) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.setColor(Color.YELLOW);
        g.fillOval(width / 4, height / 4, width / 2, height / 2);
        g.dispose();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        return baos.toByteArray();
    }

    private static BufferedImage decode(byte[] data) throws IOException {
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
        if (img == null) {
            throw new IOException("Failed to decode image (" + data.length + " bytes)");
        }
        return img;
    }

    private static boolean isPng(byte[] data) {
        return data.length >= 8
                && data[0] == (byte) 0x89
                && data[1] == (byte) 0x50
                && data[2] == (byte) 0x4E
                && data[3] == (byte) 0x47;
    }

    private static boolean isJpeg(byte[] data) {
        return data.length >= 2
                && data[0] == (byte) 0xFF
                && data[1] == (byte) 0xD8;
    }
}
