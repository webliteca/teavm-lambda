package ca.weblite.teavmlambda.api.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Security context populated from a validated JWT token.
 * <p>
 * Provides access to the authenticated user's identity, roles, and claims.
 * Resource methods can declare a {@code SecurityContext} parameter to receive
 * the authenticated context — the annotation processor generates the wiring.
 */
public final class SecurityContext {

    private final String subject;
    private final Set<String> roles;
    private final Map<String, String> claims;

    public SecurityContext(String subject, Set<String> roles, Map<String, String> claims) {
        this.subject = subject;
        this.roles = roles != null ? Collections.unmodifiableSet(new HashSet<>(roles)) : Collections.emptySet();
        this.claims = claims != null ? Collections.unmodifiableMap(new HashMap<>(claims)) : Collections.emptyMap();
    }

    /**
     * Returns the subject (user identity) from the JWT {@code sub} claim.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the user's name, preferring {@code upn}, then {@code preferred_username},
     * then falling back to {@code sub}.
     */
    public String getName() {
        String upn = claims.get("upn");
        if (upn != null && !upn.isEmpty()) return upn;
        String preferred = claims.get("preferred_username");
        if (preferred != null && !preferred.isEmpty()) return preferred;
        return subject;
    }

    /**
     * Returns the set of roles from the JWT {@code groups} claim.
     */
    public Set<String> getRoles() {
        return roles;
    }

    /**
     * Returns true if the authenticated user has the specified role.
     */
    public boolean isUserInRole(String role) {
        return roles.contains(role);
    }

    /**
     * Returns a specific claim value, or {@code null} if not present.
     */
    public String getClaim(String name) {
        return claims.get(name);
    }

    /**
     * Returns all claims as an unmodifiable map.
     */
    public Map<String, String> getClaims() {
        return claims;
    }
}
