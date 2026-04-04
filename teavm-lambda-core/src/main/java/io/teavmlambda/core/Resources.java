package io.teavmlambda.core;

import org.teavm.jso.JSBody;

/**
 * Loads classpath-style resources from the filesystem at runtime.
 * Resources are read from a {@code resources/} directory relative to
 * the working directory, mirroring how {@code getResourceAsStream}
 * works on the JVM but backed by Node.js {@code fs}.
 */
public final class Resources {

    private Resources() {
    }

    /**
     * Loads a resource as text.
     *
     * @param name resource path, e.g. {@code "/openapi.json"} or {@code "openapi.json"}
     * @return the content as a string, or {@code null} if the resource does not exist
     */
    public static String loadText(String name) {
        if (name == null) {
            return null;
        }
        String path = name.startsWith("/") ? name.substring(1) : name;
        return readFile(path);
    }

    @JSBody(params = {"relativePath"}, script = ""
            + "var fs = require('fs');"
            + "var path = require('path');"
            + "var filePath = path.join(process.cwd(), 'resources', relativePath);"
            + "try { return fs.readFileSync(filePath, 'utf8'); }"
            + "catch(e) { return null; }")
    private static native String readFile(String relativePath);
}
