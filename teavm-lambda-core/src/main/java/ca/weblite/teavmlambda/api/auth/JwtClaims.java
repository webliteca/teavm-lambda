package ca.weblite.teavmlambda.api.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Decoded claims from a validated JWT token.
 */
public final class JwtClaims {

    private final String subject;
    private final String issuer;
    private final long expirationTime;
    private final long issuedAt;
    private final Set<String> groups;
    private final Map<String, String> allClaims;

    public JwtClaims(String subject, String issuer, long expirationTime, long issuedAt,
                     Set<String> groups, Map<String, String> allClaims) {
        this.subject = subject;
        this.issuer = issuer;
        this.expirationTime = expirationTime;
        this.issuedAt = issuedAt;
        this.groups = groups != null ? Collections.unmodifiableSet(new HashSet<>(groups)) : Collections.emptySet();
        this.allClaims = allClaims != null ? Collections.unmodifiableMap(new HashMap<>(allClaims)) : Collections.emptyMap();
    }

    public String getSubject() {
        return subject;
    }

    public String getIssuer() {
        return issuer;
    }

    /** Expiration time in seconds since epoch. */
    public long getExpirationTime() {
        return expirationTime;
    }

    /** Issued-at time in seconds since epoch. */
    public long getIssuedAt() {
        return issuedAt;
    }

    /** The {@code groups} claim, used for role-based access control. */
    public Set<String> getGroups() {
        return groups;
    }

    /** Returns a specific claim value, or {@code null} if not present. */
    public String getClaim(String name) {
        return allClaims.get(name);
    }

    /** Returns all claims as an unmodifiable map. */
    public Map<String, String> getAllClaims() {
        return allClaims;
    }

    /**
     * Converts these claims into a {@link SecurityContext}.
     */
    public SecurityContext toSecurityContext() {
        return new SecurityContext(subject, groups, allClaims);
    }
}
