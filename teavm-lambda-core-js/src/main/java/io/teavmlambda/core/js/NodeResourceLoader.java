package io.teavmlambda.core.js;

import io.teavmlambda.core.ResourceLoader;
import org.teavm.jso.JSBody;

/**
 * ResourceLoader implementation that reads from the Node.js filesystem.
 * Reads from a {@code resources/} directory relative to {@code process.cwd()}.
 */
public class NodeResourceLoader implements ResourceLoader {

    @Override
    public String loadText(String name) {
        return readFile(name);
    }

    @JSBody(params = {"relativePath"}, script = ""
            + "var fs = require('fs');"
            + "var path = require('path');"
            + "var filePath = path.join(process.cwd(), 'resources', relativePath);"
            + "try { return fs.readFileSync(filePath, 'utf8'); }"
            + "catch(e) { return null; }")
    private static native String readFile(String relativePath);

    /**
     * Installs this loader as the active ResourceLoader.
     * Call this early in your application startup (e.g. in Main).
     */
    public static void install() {
        io.teavmlambda.core.Resources.setLoader(new NodeResourceLoader());
    }
}
