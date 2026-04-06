package ca.weblite.teavmlambda.impl.jvm.compression;

import ca.weblite.teavmlambda.api.compression.Compressor;
import ca.weblite.teavmlambda.api.compression.CompressorProvider;

public final class JvmCompressorProvider implements CompressorProvider {

    @Override
    public Compressor create() {
        return new JvmCompressor();
    }
}
