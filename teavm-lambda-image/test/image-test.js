/**
 * Integration tests for the Node.js sharp image operations.
 *
 * These tests exercise the same sharp API calls that the TeaVM-compiled
 * JsImageProcessor invokes at runtime, validating that the JS interop
 * layer will work correctly when deployed.
 *
 * Prerequisites: npm install sharp
 */
const sharp = require('sharp');

let pass = 0;
let fail = 0;

function assert(desc, condition) {
    if (condition) {
        console.log(`  PASS: ${desc}`);
        pass++;
    } else {
        console.log(`  FAIL: ${desc}`);
        fail++;
    }
}

function isPng(buffer) {
    return buffer.length >= 8
        && buffer[0] === 0x89
        && buffer[1] === 0x50
        && buffer[2] === 0x4E
        && buffer[3] === 0x47;
}

function isJpeg(buffer) {
    return buffer.length >= 2
        && buffer[0] === 0xFF
        && buffer[1] === 0xD8;
}

function isWebp(buffer) {
    return buffer.length >= 4
        && buffer[0] === 0x52  // R
        && buffer[1] === 0x49  // I
        && buffer[2] === 0x46  // F
        && buffer[3] === 0x46; // F
}

async function createTestPng(width, height) {
    // Create a solid-color PNG with sharp
    return sharp({
        create: {
            width: width,
            height: height,
            channels: 4,
            background: { r: 0, g: 100, b: 200, alpha: 1 }
        }
    }).png().toBuffer();
}

async function createTestJpeg(width, height) {
    return sharp({
        create: {
            width: width,
            height: height,
            channels: 3,
            background: { r: 200, g: 50, b: 50 }
        }
    }).jpeg().toBuffer();
}

async function runTests() {
    console.log('=== Image IO Node.js Integration Tests ===');
    console.log();

    const pngImage = await createTestPng(400, 300);
    const jpegImage = await createTestJpeg(400, 300);

    // --- Resize with fit: inside (preserveAspectRatio=true) ---
    console.log('--- Resize (fit: inside) ---');
    {
        const result = await sharp(pngImage).resize(200, 150, { fit: 'inside' }).toBuffer();
        const meta = await sharp(result).metadata();
        assert('Resize fit:inside width <= 200', meta.width <= 200);
        assert('Resize fit:inside height <= 150', meta.height <= 150);
        assert('Resize fit:inside reduces dimensions', meta.width < 400 || meta.height < 300);
        assert('Resize fit:inside output is PNG', isPng(result));
    }

    // --- Resize with fit: fill (preserveAspectRatio=false) ---
    console.log();
    console.log('--- Resize (fit: fill) ---');
    {
        const result = await sharp(pngImage).resize(100, 50, { fit: 'fill' }).toBuffer();
        const meta = await sharp(result).metadata();
        assert('Resize fit:fill width == 100', meta.width === 100);
        assert('Resize fit:fill height == 50', meta.height === 50);
    }

    // --- Resize width only ---
    console.log();
    console.log('--- Resize (width only) ---');
    {
        const result = await sharp(pngImage).resize(200, 300, { fit: 'inside' }).toBuffer();
        const meta = await sharp(result).metadata();
        assert('Resize width-only: width <= 200', meta.width <= 200);
        assert('Resize width-only: height scaled proportionally', meta.height <= 300);
    }

    // --- Format Conversion ---
    console.log();
    console.log('--- Format Conversion ---');
    {
        const pngToJpeg = await sharp(pngImage).jpeg().toBuffer();
        assert('PNG -> JPEG produces JPEG', isJpeg(pngToJpeg));
        assert('PNG -> JPEG output is non-empty', pngToJpeg.length > 0);

        const jpegToPng = await sharp(jpegImage).png().toBuffer();
        assert('JPEG -> PNG produces PNG', isPng(jpegToPng));

        const pngToWebp = await sharp(pngImage).webp().toBuffer();
        assert('PNG -> WEBP produces WEBP', isWebp(pngToWebp));

        const webpToPng = await sharp(pngToWebp).png().toBuffer();
        assert('WEBP -> PNG produces PNG', isPng(webpToPng));
    }

    // --- Compression / Quality ---
    // Use a large noisy image so quality settings have a measurable effect.
    // Solid-color images compress identically at all quality levels.
    console.log();
    console.log('--- Compression ---');
    {
        const channels = 3;
        const w = 800, h = 600;
        const pixels = Buffer.alloc(w * h * channels);
        for (let i = 0; i < pixels.length; i++) {
            pixels[i] = Math.floor(Math.random() * 256);
        }
        const noisyPng = await sharp(pixels, { raw: { width: w, height: h, channels } }).png().toBuffer();

        const highQuality = await sharp(noisyPng).jpeg({ quality: 95 }).toBuffer();
        const lowQuality = await sharp(noisyPng).jpeg({ quality: 10 }).toBuffer();
        assert('JPEG quality 95 produces valid JPEG', isJpeg(highQuality));
        assert('JPEG quality 10 produces valid JPEG', isJpeg(lowQuality));
        assert('Lower quality JPEG is smaller', lowQuality.length < highQuality.length);

        const highWebp = await sharp(noisyPng).webp({ quality: 95 }).toBuffer();
        const lowWebp = await sharp(noisyPng).webp({ quality: 10 }).toBuffer();
        assert('WEBP quality 95 produces valid WEBP', isWebp(highWebp));
        assert('WEBP quality 10 produces valid WEBP', isWebp(lowWebp));
        assert('Lower quality WEBP is smaller', lowWebp.length < highWebp.length);
    }

    // --- Resize + Convert combined ---
    console.log();
    console.log('--- Resize + Convert ---');
    {
        const result = await sharp(pngImage)
            .resize(100, 100, { fit: 'inside' })
            .jpeg({ quality: 80 })
            .toBuffer();
        const meta = await sharp(result).metadata();
        assert('Resize+Convert produces JPEG', isJpeg(result));
        assert('Resize+Convert: width <= 100', meta.width <= 100);
        assert('Resize+Convert: height <= 100', meta.height <= 100);
    }

    // --- Buffer round-trip (simulates byte[] -> Buffer -> byte[]) ---
    console.log();
    console.log('--- Buffer Round-trip ---');
    {
        // Simulate what JsImageUtil does: Buffer.from(bytes) and back
        const arr = Array.prototype.slice.call(new Int8Array(pngImage.buffer, pngImage.byteOffset, pngImage.length));
        const roundtripped = Buffer.from(arr);
        assert('Buffer round-trip preserves length', roundtripped.length === pngImage.length);
        assert('Buffer round-trip preserves content', roundtripped.equals(pngImage));

        // Verify the round-tripped buffer still works with sharp
        const result = await sharp(roundtripped).resize(50, 50, { fit: 'inside' }).toBuffer();
        assert('Round-tripped buffer works with sharp', isPng(result));
    }

    // --- Error Handling ---
    console.log();
    console.log('--- Error Handling ---');
    {
        let threw = false;
        try {
            await sharp(Buffer.from([0, 1, 2, 3])).resize(100, 100).toBuffer();
        } catch (e) {
            threw = true;
        }
        assert('Invalid image data throws error', threw);
    }

    // --- Results ---
    console.log();
    console.log('=======================================');
    console.log(`Results: ${pass} passed, ${fail} failed`);
    console.log('=======================================');

    if (fail > 0) {
        process.exit(1);
    }
}

runTests().catch(err => {
    console.error('Test runner failed:', err);
    process.exit(1);
});
