package ca.weblite.teavmlambda.impl.js.compression;

import ca.weblite.teavmlambda.api.compression.Compressor;
import ca.weblite.teavmlambda.api.compression.CompressorProvider;

public final class JsCompressorProvider implements CompressorProvider {

    @Override
    public Compressor create() {
        return new JsCompressor();
    }
}
