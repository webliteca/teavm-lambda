package ca.weblite.teavmlambda.impl.jvm.auth;

import ca.weblite.teavmlambda.api.auth.JwtClaims;
import ca.weblite.teavmlambda.api.auth.JwtValidationException;
import ca.weblite.teavmlambda.api.auth.JwtValidator;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JVM implementation of {@link JwtValidator}.
 * Supports HMAC-SHA256/384/512 (HS256/HS384/HS512) and RSA-SHA256 (RS256) signature verification.
 * For Firebase Auth, fetches public keys from Google's endpoint and validates Firebase-specific claims.
 */
public class JvmJwtValidator implements JwtValidator {

    private final String secret;
    private final String issuer;
    private final String algorithm;
    private final String firebaseProjectId;

    public JvmJwtValidator(String secret, String issuer, String algorithm) {
        this.secret = secret;
        this.issuer = issuer;
        this.algorithm = algorithm != null ? algorithm : "HS256";
        // For Firebase mode, secret holds the project ID
        this.firebaseProjectId = "firebase".equalsIgnoreCase(this.algorithm) ? secret : null;
    }

    @Override
    public JwtClaims validate(String token) throws JwtValidationException {
        if (token == null || token.isEmpty()) {
            throw new JwtValidationException("Token is null or empty");
        }

        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new JwtValidationException("Invalid JWT format: expected 3 parts, got " + parts.length);
        }

        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];
        String signingInput = header + "." + payload;

        if ("firebase".equalsIgnoreCase(algorithm) || "RS256".equalsIgnoreCase(algorithm)) {
            verifyRs256(header, signingInput, signature);
        } else {
            // HMAC verification (HS256/HS384/HS512)
            String expected = computeHmac(signingInput, secret, algorithm);
            if (!constantTimeEquals(expected, signature)) {
                throw new JwtValidationException("Invalid JWT signature");
            }
        }

        // Decode payload
        String payloadJson = new String(
                Base64.getUrlDecoder().decode(payload), StandardCharsets.UTF_8);

        return parseClaims(payloadJson);
    }

    private void verifyRs256(String headerB64, String signingInput, String signature) throws JwtValidationException {
        String headerJson = new String(
                Base64.getUrlDecoder().decode(headerB64), StandardCharsets.UTF_8);

        // Extract kid from header
        String kid = extractString(headerJson, "kid");

        // Get the public key
        PublicKey publicKey;
        if (firebaseProjectId != null) {
            // Firebase mode: fetch key from Google endpoint by kid
            if (kid == null || kid.isEmpty()) {
                throw new JwtValidationException("JWT header missing 'kid' for RS256 verification");
            }
            publicKey = JvmFirebaseKeyFetcher.getPublicKey(kid);
            if (publicKey == null) {
                throw new JwtValidationException("No public key found for kid: " + kid);
            }
        } else {
            // Static RS256: parse the configured PEM public key
            try {
                publicKey = JvmFirebaseKeyFetcher.parsePublicKeyFromPem(secret);
            } catch (Exception e) {
                throw new JwtValidationException("Failed to parse RS256 public key: " + e.getMessage(), e);
            }
        }

        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(publicKey);
            sig.update(signingInput.getBytes(StandardCharsets.UTF_8));

            // Decode base64url signature
            String b64 = signature.replace('-', '+').replace('_', '/');
            int pad = b64.length() % 4;
            if (pad > 0) b64 += "====".substring(pad);
            byte[] sigBytes = Base64.getDecoder().decode(b64);

            if (!sig.verify(sigBytes)) {
                throw new JwtValidationException("Invalid JWT signature");
            }
        } catch (JwtValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtValidationException("RS256 signature verification failed: " + e.getMessage(), e);
        }
    }

    private String computeHmac(String data, String secret, String algorithm) throws JwtValidationException {
        try {
            String hmacAlg;
            switch (algorithm) {
                case "HS256": hmacAlg = "HmacSHA256"; break;
                case "HS384": hmacAlg = "HmacSHA384"; break;
                case "HS512": hmacAlg = "HmacSHA512"; break;
                default:
                    throw new JwtValidationException("Unsupported algorithm: " + algorithm);
            }
            Mac mac = Mac.getInstance(hmacAlg);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), hmacAlg));
            byte[] sig = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(sig);
        } catch (JwtValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtValidationException("Failed to compute HMAC: " + e.getMessage(), e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) return false;
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
    }

    private JwtClaims parseClaims(String json) throws JwtValidationException {
        // Minimal JSON parsing — no external dependencies
        String sub = extractString(json, "sub");
        String iss = extractString(json, "iss");
        long exp = extractLong(json, "exp");
        long iat = extractLong(json, "iat");

        // Check expiration
        long nowSeconds = System.currentTimeMillis() / 1000;
        if (exp > 0 && nowSeconds > exp) {
            throw new JwtValidationException("Token has expired");
        }

        // Check issuer
        if (issuer != null && !issuer.equals(iss)) {
            throw new JwtValidationException("Invalid issuer: expected " + issuer + ", got " + iss);
        }

        // Firebase-specific validations
        if (firebaseProjectId != null) {
            // Verify audience matches project ID
            String aud = extractString(json, "aud");
            if (!firebaseProjectId.equals(aud)) {
                throw new JwtValidationException(
                        "Invalid audience: expected " + firebaseProjectId + ", got " + aud);
            }
            // Verify sub is non-empty (Firebase UID)
            if (sub == null || sub.isEmpty()) {
                throw new JwtValidationException("Firebase token missing 'sub' claim (user ID)");
            }
        }

        // Extract groups
        Set<String> groups = extractStringArray(json, "groups");

        // For Firebase tokens, add default "user" role and extract custom claims
        if (firebaseProjectId != null) {
            groups = new HashSet<>(groups); // make mutable
            groups.add("user");
            // Firebase custom claims may include a "roles" array
            Set<String> roles = extractStringArray(json, "roles");
            groups.addAll(roles);
        }

        // Build all claims map
        Map<String, String> allClaims = new HashMap<>();
        if (sub != null) allClaims.put("sub", sub);
        if (iss != null) allClaims.put("iss", iss);
        if (exp > 0) allClaims.put("exp", String.valueOf(exp));
        if (iat > 0) allClaims.put("iat", String.valueOf(iat));

        String upn = extractString(json, "upn");
        if (upn != null) allClaims.put("upn", upn);
        String preferredUsername = extractString(json, "preferred_username");
        if (preferredUsername != null) allClaims.put("preferred_username", preferredUsername);
        String email = extractString(json, "email");
        if (email != null) allClaims.put("email", email);
        String name = extractString(json, "name");
        if (name != null) allClaims.put("name", name);
        String emailVerified = extractString(json, "email_verified");
        if (emailVerified != null) allClaims.put("email_verified", emailVerified);
        String aud = extractString(json, "aud");
        if (aud != null) allClaims.put("aud", aud);

        return new JwtClaims(sub, iss, exp, iat, groups, allClaims);
    }

    // --- Minimal JSON extraction (no external library) ---

    private static String extractString(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;

        int colonIdx = json.indexOf(':', keyIdx + search.length());
        if (colonIdx < 0) return null;

        // Skip whitespace after colon
        int valueStart = colonIdx + 1;
        while (valueStart < json.length() && json.charAt(valueStart) == ' ') valueStart++;

        if (valueStart >= json.length()) return null;

        char c = json.charAt(valueStart);
        if (c == '"') {
            // String value
            int valueEnd = findClosingQuote(json, valueStart + 1);
            if (valueEnd < 0) return null;
            return unescapeJson(json.substring(valueStart + 1, valueEnd));
        } else if (c == 'n') {
            return null; // null
        } else {
            // Number or boolean — read until , } or whitespace
            int valueEnd = valueStart;
            while (valueEnd < json.length()) {
                char ch = json.charAt(valueEnd);
                if (ch == ',' || ch == '}' || ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') break;
                valueEnd++;
            }
            return json.substring(valueStart, valueEnd);
        }
    }

    private static long extractLong(String json, String key) {
        String value = extractString(json, key);
        if (value == null) return 0;
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static Set<String> extractStringArray(String json, String key) {
        Set<String> result = new HashSet<>();
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return result;

        int bracketStart = json.indexOf('[', keyIdx + search.length());
        if (bracketStart < 0) return result;

        int bracketEnd = json.indexOf(']', bracketStart);
        if (bracketEnd < 0) return result;

        String arrayContent = json.substring(bracketStart + 1, bracketEnd);
        int i = 0;
        while (i < arrayContent.length()) {
            int quoteStart = arrayContent.indexOf('"', i);
            if (quoteStart < 0) break;
            int quoteEnd = findClosingQuote(arrayContent, quoteStart + 1);
            if (quoteEnd < 0) break;
            result.add(unescapeJson(arrayContent.substring(quoteStart + 1, quoteEnd)));
            i = quoteEnd + 1;
        }
        return result;
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
