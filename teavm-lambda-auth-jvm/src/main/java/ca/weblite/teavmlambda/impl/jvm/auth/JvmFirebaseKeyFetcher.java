package ca.weblite.teavmlambda.impl.jvm.auth;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Fetches and caches Firebase/Google public keys for RS256 JWT verification.
 * Uses {@link HttpURLConnection} to fetch X.509 certificates from Google's endpoint.
 * Keys are cached based on the {@code Cache-Control: max-age} header.
 */
final class JvmFirebaseKeyFetcher {

    private static final String GOOGLE_CERTS_URL =
            "https://www.googleapis.com/robot/v1/metadata/x509/securetoken@system.gserviceaccount.com";

    /** Cached kid -> PublicKey mappings */
    private static Map<String, PublicKey> cachedKeys;
    /** Epoch millis when cache expires */
    private static long cacheExpiresAt;

    private JvmFirebaseKeyFetcher() {
    }

    /**
     * Returns the public key for the given key ID.
     * Fetches from Google if the cache is expired.
     *
     * @param kid the key ID from the JWT header
     * @return the RSA public key, or null if kid not found
     */
    static PublicKey getPublicKey(String kid) {
        long now = System.currentTimeMillis();
        if (cachedKeys == null || now >= cacheExpiresAt) {
            refreshKeys();
        }
        if (cachedKeys == null) {
            return null;
        }
        return cachedKeys.get(kid);
    }

    /**
     * Allows tests to inject keys without hitting the network.
     */
    static void setKeysForTesting(Map<String, PublicKey> keys, long expiresAt) {
        cachedKeys = keys;
        cacheExpiresAt = expiresAt;
    }

    /**
     * Clears the cached keys.
     */
    static void clearCache() {
        cachedKeys = null;
        cacheExpiresAt = 0;
    }

    private static void refreshKeys() {
        try {
            URL url = java.net.URI.create(GOOGLE_CERTS_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int status = conn.getResponseCode();
            if (status != 200) {
                if (cachedKeys != null) return; // keep stale cache
                throw new RuntimeException("Failed to fetch Firebase public keys: HTTP " + status);
            }

            // Parse max-age from Cache-Control header
            long maxAge = 21600; // default 6 hours
            String cacheControl = conn.getHeaderField("Cache-Control");
            if (cacheControl != null) {
                int idx = cacheControl.indexOf("max-age=");
                if (idx >= 0) {
                    int start = idx + 8;
                    int end = start;
                    while (end < cacheControl.length() && Character.isDigit(cacheControl.charAt(end))) {
                        end++;
                    }
                    if (end > start) {
                        maxAge = Long.parseLong(cacheControl.substring(start, end));
                    }
                }
            }

            // Read response body
            InputStream is = conn.getInputStream();
            byte[] buf = new byte[8192];
            StringBuilder sb = new StringBuilder();
            int n;
            while ((n = is.read(buf)) != -1) {
                sb.append(new String(buf, 0, n, StandardCharsets.UTF_8));
            }
            is.close();
            conn.disconnect();

            String body = sb.toString();

            // Parse JSON: { "kid1": "-----BEGIN CERTIFICATE-----\n...", "kid2": "..." }
            Map<String, PublicKey> keys = parseCertificateJson(body);
            cachedKeys = keys;
            cacheExpiresAt = System.currentTimeMillis() + (maxAge * 1000);

        } catch (RuntimeException e) {
            if (cachedKeys == null) throw e;
            // Keep stale cache on error
        } catch (Exception e) {
            if (cachedKeys == null) {
                throw new RuntimeException("Failed to fetch Firebase public keys: " + e.getMessage(), e);
            }
            // Keep stale cache on error
        }
    }

    /**
     * Parses a JSON object mapping kid to PEM-encoded X.509 certificates,
     * and extracts the RSA public keys.
     */
    private static Map<String, PublicKey> parseCertificateJson(String json) {
        Map<String, PublicKey> result = new HashMap<>();
        CertificateFactory cf;
        try {
            cf = CertificateFactory.getInstance("X.509");
        } catch (Exception e) {
            throw new RuntimeException("X.509 CertificateFactory not available", e);
        }

        // Minimal JSON parsing for { "key": "value", ... }
        int i = json.indexOf('{');
        if (i < 0) return result;
        i++;

        while (i < json.length()) {
            // Find next key
            int keyStart = json.indexOf('"', i);
            if (keyStart < 0) break;
            int keyEnd = findClosingQuote(json, keyStart + 1);
            if (keyEnd < 0) break;
            String kid = json.substring(keyStart + 1, keyEnd);

            // Find value
            int colonIdx = json.indexOf(':', keyEnd + 1);
            if (colonIdx < 0) break;
            int valStart = json.indexOf('"', colonIdx + 1);
            if (valStart < 0) break;
            int valEnd = findClosingQuote(json, valStart + 1);
            if (valEnd < 0) break;
            String pem = unescapeJson(json.substring(valStart + 1, valEnd));

            try {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
                result.put(kid, cert.getPublicKey());
            } catch (Exception e) {
                // Skip invalid certificates
            }

            i = valEnd + 1;
        }

        return result;
    }

    /**
     * Parses a PEM-encoded X.509 certificate and extracts the public key.
     */
    static PublicKey parsePublicKeyFromPem(String pem) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            if (pem.contains("BEGIN CERTIFICATE")) {
                X509Certificate cert = (X509Certificate) cf.generateCertificate(
                        new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
                return cert.getPublicKey();
            }
            // Handle raw PEM public key
            String stripped = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s+", "");
            byte[] decoded = java.util.Base64.getDecoder().decode(stripped);
            java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(decoded);
            return java.security.KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse public key from PEM: " + e.getMessage(), e);
        }
    }

    private static int findClosingQuote(String s, int from) {
        for (int i = from; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++; // skip escaped character
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private static String unescapeJson(String s) {
        if (s.indexOf('\\') < 0) return s;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char next = s.charAt(i + 1);
                switch (next) {
                    case '"': case '\\': case '/': sb.append(next); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    default: sb.append(c).append(next); break;
                }
                i++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
