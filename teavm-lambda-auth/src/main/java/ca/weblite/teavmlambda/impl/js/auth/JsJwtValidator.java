package ca.weblite.teavmlambda.impl.js.auth;

import ca.weblite.teavmlambda.api.auth.JwtClaims;
import ca.weblite.teavmlambda.api.auth.JwtValidationException;
import ca.weblite.teavmlambda.api.auth.JwtValidator;
import org.teavm.jso.JSBody;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Node.js/TeaVM implementation of {@link JwtValidator}.
 * Uses Node.js {@code crypto} module for HMAC-SHA256 signature verification
 * and built-in {@code atob}/{@code Buffer} for Base64 decoding.
 */
public class JsJwtValidator implements JwtValidator {

    private final String secret;
    private final String issuer;
    private final String algorithm;

    public JsJwtValidator(String secret, String issuer, String algorithm) {
        this.secret = secret;
        this.issuer = issuer;
        this.algorithm = algorithm != null ? algorithm : "HS256";
    }

    @Override
    public JwtClaims validate(String token) throws JwtValidationException {
        if (token == null || token.isEmpty()) {
            throw new JwtValidationException("Token is null or empty");
        }

        // Split token into parts
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtValidationException("Invalid JWT format: expected 3 parts, got " + parts.length);
        }

        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        // Verify signature
        String signingInput = header + "." + payload;
        if (!verifySignature(signingInput, signature, secret, algorithm)) {
            throw new JwtValidationException("Invalid JWT signature");
        }

        // Decode payload
        String payloadJson = base64UrlDecode(payload);
        return parseClaims(payloadJson);
    }

    private JwtClaims parseClaims(String json) throws JwtValidationException {
        String sub = extractJsonString(json, "sub");
        String iss = extractJsonString(json, "iss");
        long exp = (long) extractJsonDouble(json, "exp");
        long iat = (long) extractJsonDouble(json, "iat");

        // Check expiration
        long nowSeconds = System.currentTimeMillis() / 1000;
        if (exp > 0 && nowSeconds > exp) {
            throw new JwtValidationException("Token has expired");
        }

        // Check issuer
        if (issuer != null && !issuer.equals(iss)) {
            throw new JwtValidationException("Invalid issuer: expected " + issuer + ", got " + iss);
        }

        // Extract groups
        Set<String> groups = new HashSet<>();
        String groupsStr = extractJsonArray(json, "groups");
        if (groupsStr != null && !groupsStr.isEmpty()) {
            for (String g : groupsStr.split(",")) {
                String trimmed = g.trim();
                if (!trimmed.isEmpty()) {
                    groups.add(trimmed);
                }
            }
        }

        // Build all claims map
        Map<String, String> allClaims = new HashMap<>();
        if (sub != null) allClaims.put("sub", sub);
        if (iss != null) allClaims.put("iss", iss);
        if (exp > 0) allClaims.put("exp", String.valueOf(exp));
        if (iat > 0) allClaims.put("iat", String.valueOf(iat));

        // Extract common optional claims
        String upn = extractJsonString(json, "upn");
        if (upn != null) allClaims.put("upn", upn);
        String preferredUsername = extractJsonString(json, "preferred_username");
        if (preferredUsername != null) allClaims.put("preferred_username", preferredUsername);
        String email = extractJsonString(json, "email");
        if (email != null) allClaims.put("email", email);
        String name = extractJsonString(json, "name");
        if (name != null) allClaims.put("name", name);

        return new JwtClaims(sub, iss, exp, iat, groups, allClaims);
    }

    // --- JS interop via @JSBody ---

    @JSBody(params = {"signingInput", "signature", "secret", "algorithm"}, script = ""
            + "var crypto = require('crypto');"
            + "var alg = 'sha256';"
            + "if (algorithm === 'HS384') alg = 'sha384';"
            + "else if (algorithm === 'HS512') alg = 'sha512';"
            + "var hmac = crypto.createHmac(alg, secret);"
            + "hmac.update(signingInput);"
            + "var expected = hmac.digest('base64url');"
            + "return expected === signature;")
    private static native boolean verifySignature(String signingInput, String signature,
                                                   String secret, String algorithm);

    @JSBody(params = {"base64url"}, script = ""
            + "var base64 = base64url.replace(/-/g, '+').replace(/_/g, '/');"
            + "var pad = base64.length % 4;"
            + "if (pad) base64 += '='.repeat(4 - pad);"
            + "return Buffer.from(base64, 'base64').toString('utf8');")
    private static native String base64UrlDecode(String base64url);

    @JSBody(params = {"json", "key"}, script = ""
            + "try { var obj = JSON.parse(json); return obj[key] != null ? String(obj[key]) : null; }"
            + "catch(e) { return null; }")
    private static native String extractJsonString(String json, String key);

    @JSBody(params = {"json", "key"}, script = ""
            + "try { var obj = JSON.parse(json); return obj[key] != null ? Number(obj[key]) : 0; }"
            + "catch(e) { return 0; }")
    private static native double extractJsonDouble(String json, String key);

    @JSBody(params = {"json", "key"}, script = ""
            + "try {"
            + "  var obj = JSON.parse(json);"
            + "  var arr = obj[key];"
            + "  if (!Array.isArray(arr)) return null;"
            + "  return arr.join(',');"
            + "} catch(e) { return null; }")
    private static native String extractJsonArray(String json, String key);
}
