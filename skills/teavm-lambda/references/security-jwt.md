# Security and JWT Authentication

> Read this when the user needs JWT-based authentication, role-based access control, Firebase Auth, or SecurityContext.

## Setup

```java
Container container = new GeneratedContainer();
container.register(JwtValidator.class, JwtValidatorFactory.create());
Router router = new GeneratedRouter(container);
```

`JwtValidatorFactory.create()` reads configuration from environment variables.

**Required modules**:
- Node.js: `teavm-lambda-auth`
- JVM: `teavm-lambda-auth-jvm`

## Environment Variables

| Variable | Purpose | Example |
|----------|---------|---------|
| `JWT_SECRET` | HMAC secret key (HS256/HS384/HS512) | `my-secret-key` |
| `JWT_PUBLIC_KEY` | PEM public key (RS256/ES256) | `-----BEGIN PUBLIC KEY-----...` |
| `JWT_ISSUER` | Expected issuer claim (optional) | `https://auth.example.com` |
| `JWT_ALGORITHM` | Algorithm name | `HS256`, `RS256`, `firebase` |
| `FIREBASE_PROJECT_ID` | Firebase project ID (when using Firebase Auth) | `my-firebase-project` |

### Algorithm selection

- **Default (no JWT_ALGORITHM)**: HS256 with JWT_SECRET
- **RS256**: Set `JWT_ALGORITHM=RS256` + `JWT_PUBLIC_KEY` (PEM)
- **Firebase Auth**: Set `JWT_ALGORITHM=firebase` + `FIREBASE_PROJECT_ID`
  - Issuer auto-set to `https://securetoken.google.com/<project-id>`
  - Public keys fetched and cached from Google's X.509 endpoint
  - Validates `aud` against project ID
  - All authenticated users get the `user` role

## Security Annotations

### @RolesAllowed

Requires the authenticated user to have at least one of the listed roles (from JWT "groups" claim).

```java
@Path("/api")
@Component @Singleton
@RolesAllowed({"user", "admin"})   // class-level: applies to all methods
public class ProtectedResource {

    @GET
    public Response list(SecurityContext ctx) {
        // Only users with "user" or "admin" role reach here
        return Response.ok("Hello " + ctx.getName());
    }

    @DELETE
    @Path("/{id}")
    @RolesAllowed({"admin"})   // method-level: overrides class-level
    public Response delete(@PathParam("id") String id) {
        // Only admins can delete
        return Response.status(204);
    }
}
```

### @PermitAll

Allows unauthenticated access. Use on a method to override class-level restrictions:

```java
@Path("/api")
@Component @Singleton
@RolesAllowed({"user"})
public class MixedResource {

    @GET
    @Path("/public")
    @PermitAll                     // overrides class-level @RolesAllowed
    public Response publicEndpoint() {
        return Response.ok("{\"status\":\"public\"}");
    }

    @GET
    @Path("/private")
    public Response privateEndpoint(SecurityContext ctx) {
        return Response.ok("{\"user\":\"" + ctx.getSubject() + "\"}");
    }
}
```

### @DenyAll

Denies all access (always returns 403 Forbidden):

```java
@DELETE
@Path("/dangerous")
@DenyAll
public Response neverAllowed() {
    return Response.status(204);  // never reached
}
```

## SecurityContext

Resource methods can declare a `SecurityContext` parameter to receive the authenticated user's identity. The annotation processor generates the wiring automatically.

```java
@GET
public Response profile(SecurityContext ctx) {
    String userId = ctx.getSubject();         // JWT "sub" claim
    String name = ctx.getName();              // "upn" or "preferred_username" or "sub"
    Set<String> roles = ctx.getRoles();       // JWT "groups" claim
    boolean isAdmin = ctx.isUserInRole("admin");
    String email = ctx.getClaim("email");     // any custom claim
    Map<String,String> allClaims = ctx.getClaims();

    return Response.ok(JsonBuilder.object()
            .put("id", userId)
            .put("name", name)
            .put("admin", isAdmin)
            .build())
            .header("Content-Type", "application/json");
}
```

## Error Responses

| Status | When |
|--------|------|
| 401 Unauthorized | Missing token, expired token, invalid signature |
| 403 Forbidden | Valid token but insufficient roles, or @DenyAll |

## Complete Auth Demo

```java
// Main.java
public class Main {
    public static void main(String[] args) {
        Container container = new GeneratedContainer();

        // Only register JwtValidator if auth modules are available
        if (JwtValidatorFactory.isAvailable()) {
            container.register(JwtValidator.class, JwtValidatorFactory.create());
        }

        Router router = new MiddlewareRouter(new GeneratedRouter(container))
                .use(CorsMiddleware.builder()
                        .allowHeaders("Content-Type", "Authorization")
                        .build());
        Platform.start(router);
    }
}

// ProtectedResource.java
@Path("/api")
@Component @Singleton
@RolesAllowed({"user"})
public class ProtectedResource {

    @GET
    @Path("/me")
    public Response me(SecurityContext ctx) {
        return Response.ok(JsonBuilder.object()
                .put("sub", ctx.getSubject())
                .put("name", ctx.getName())
                .build())
                .header("Content-Type", "application/json");
    }
}
```

Calling with curl:
```bash
# Get a JWT token from your auth provider, then:
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/me
```
