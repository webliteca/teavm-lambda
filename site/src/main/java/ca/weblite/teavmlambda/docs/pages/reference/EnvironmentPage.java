package ca.weblite.teavmlambda.docs.pages.reference;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.Div;
import ca.weblite.teavmreact.html.DomBuilder.Section;
import org.teavm.jso.JSObject;
import static ca.weblite.teavmreact.html.Html.*;
import ca.weblite.teavmlambda.docs.El;
import ca.weblite.teavmlambda.docs.components.CodeBlock;
import ca.weblite.teavmlambda.docs.components.CodeTabs;
import ca.weblite.teavmlambda.docs.components.Callout;

public class EnvironmentPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Environment Variables"))
            .child(p("teavm-lambda reads configuration from environment variables at runtime. "
                + "This follows the twelve-factor app methodology and works consistently "
                + "across all deployment targets -- AWS Lambda, Cloud Run, standalone JVM, "
                + "and Servlet containers."))
            .child(sectionVariablesTable())
            .child(sectionPlatformEnv())
            .child(sectionDatabaseUrl())
            .child(sectionJwt())
            .child(sectionDeploymentExamples())
            .build();
    }

    private static ReactElement sectionVariablesTable() {
        return Section.create().className("doc-section")
            .child(h2("Variable Reference"))
            .child(p("The following environment variables are recognized by teavm-lambda "
                + "and its bundled modules:"))
            .child(El.table("api-table",
                thead(
                    tr(th("Variable"), th("Purpose"), th("Default"))),
                tbody(
                    tr(td(code("DATABASE_URL")), td(text("PostgreSQL connection string")), td(text("none"))),
                    tr(td(code("PORT")), td(text("HTTP server port")), td(text("8080"))),
                    tr(td(code("JWT_SECRET")), td(text("HMAC signing key")), td(text("none"))),
                    tr(td(code("JWT_PUBLIC_KEY")), td(text("RSA/EC public key (PEM)")), td(text("none"))),
                    tr(td(code("JWT_ALGORITHM")), td(text("JWT algorithm (HS256, RS256, etc.)")), td(text("HS256"))),
                    tr(td(code("JWT_ISSUER")), td(text("Expected JWT issuer")), td(text("none"))),
                    tr(td(code("FIREBASE_PROJECT_ID")), td(text("Firebase project for ID token validation")), td(text("none"))),
                    tr(td(code("SENTRY_DSN")), td(text("Sentry error tracking DSN")), td(text("none"))))))
            .build();
    }

    private static ReactElement sectionPlatformEnv() {
        String javaCode = """
// Read a required variable (returns null if not set)
String dbUrl = Platform.env("DATABASE_URL");
if (dbUrl == null) {
    throw new RuntimeException("DATABASE_URL is required");
}

// Read with a default value
String port = Platform.env("PORT", "8080");
String algorithm = Platform.env("JWT_ALGORITHM", "HS256");

// Use in application startup
public static void main(String[] args) throws Exception {
    String secret = Platform.env("JWT_SECRET");
    String sentryDsn = Platform.env("SENTRY_DSN");

    var container = new GeneratedContainer();
    var router = new GeneratedRouter(container);
    var app = new MiddlewareRouter(router);

    if (secret != null) {
        app.use(new JwtMiddleware(secret));
    }
    if (sentryDsn != null) {
        app.use(new SentryMiddleware(sentryDsn));
    }

    Platform.start(app);
}""";

        String kotlinCode = """
// Read a required variable (returns null if not set)
val dbUrl = Platform.env("DATABASE_URL")
    ?: throw RuntimeException("DATABASE_URL is required")

// Read with a default value
val port = Platform.env("PORT", "8080")
val algorithm = Platform.env("JWT_ALGORITHM", "HS256")

// Use in DSL app block
app {
    val secret = Platform.env("JWT_SECRET")
    val sentryDsn = Platform.env("SENTRY_DSN")

    if (secret != null) {
        use(JwtMiddleware(secret))
    }
    if (sentryDsn != null) {
        use(SentryMiddleware(sentryDsn))
    }

    cors()
    compression()
    routes { /* ... */ }
}""";

        return Section.create().className("doc-section")
            .child(h2("Reading Environment Variables"))
            .child(p("Use Platform.env() to read environment variables portably. This "
                + "method works identically on all deployment targets. The single-argument "
                + "form returns null when the variable is not set. The two-argument form "
                + "returns a default value instead."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(Callout.pitfall("Null handling",
                p("Platform.env(name) returns null, not an empty string, when the "
                    + "variable is not set. Always check for null or use the two-argument "
                    + "form with a default value.")))
            .build();
    }

    private static ReactElement sectionDatabaseUrl() {
        return Section.create().className("doc-section")
            .child(h2("DATABASE_URL Format"))
            .child(p("The DATABASE_URL variable uses the standard PostgreSQL connection "
                + "string format. The database module parses this URL and configures "
                + "the connection automatically on both TeaVM (Node.js pg driver) and "
                + "JVM (JDBC) targets."))
            .child(CodeBlock.create(
                "postgresql://user:pass@host:port/dbname",
                "text"))
            .child(h3("Examples"))
            .child(CodeBlock.create(
                """
# Local development
DATABASE_URL=postgresql://demo:demo@localhost:5432/demo

# AWS RDS
DATABASE_URL=postgresql://myuser:mypass@mydb.abc123.us-east-1.rds.amazonaws.com:5432/myapp

# Cloud SQL (via Unix socket proxy)
DATABASE_URL=postgresql://myuser:mypass@localhost:5432/myapp""",
                "bash"))
            .child(Callout.note("Docker Compose",
                p("For local development, set DATABASE_URL in your docker-compose.yml "
                    + "environment section or in a .env file. The teavm-lambda demo "
                    + "projects include a docker-compose.yml with PostgreSQL pre-configured.")))
            .build();
    }

    private static ReactElement sectionJwt() {
        return Section.create().className("doc-section")
            .child(h2("JWT Configuration"))
            .child(p("The JWT-related environment variables configure the "
                + "teavm-lambda-auth module. You need either JWT_SECRET (for HMAC "
                + "algorithms) or JWT_PUBLIC_KEY (for RSA/EC algorithms), but not both."))
            .child(CodeBlock.create(
                """
# HMAC-based JWT (HS256, HS384, HS512)
JWT_SECRET=my-secret-key-at-least-32-bytes-long
JWT_ALGORITHM=HS256
JWT_ISSUER=https://myapp.example.com

# RSA-based JWT (RS256, RS384, RS512)
JWT_PUBLIC_KEY="-----BEGIN PUBLIC KEY-----\\nMIIBI...\\n-----END PUBLIC KEY-----"
JWT_ALGORITHM=RS256
JWT_ISSUER=https://auth.example.com

# Firebase ID token validation
FIREBASE_PROJECT_ID=my-firebase-project""",
                "bash"))
            .child(Callout.note("Firebase tokens",
                p("When FIREBASE_PROJECT_ID is set, the auth module validates Firebase "
                    + "ID tokens instead of standard JWT. The JWT_SECRET and JWT_PUBLIC_KEY "
                    + "variables are ignored in this mode.")))
            .build();
    }

    private static ReactElement sectionDeploymentExamples() {
        String lambdaCode = """
# AWS Lambda (template.yaml)
Resources:
  MyFunction:
    Type: AWS::Serverless::Function
    Properties:
      Environment:
        Variables:
          DATABASE_URL: !Sub "postgresql://${DbUser}:${DbPass}@${DbHost}:5432/${DbName}"
          JWT_SECRET: !Ref JwtSecret
          SENTRY_DSN: !Ref SentryDsn""";

        String cloudRunCode = """
# Cloud Run (deploy command)
gcloud run deploy my-app \\
  --image gcr.io/my-project/my-app \\
  --set-env-vars "DATABASE_URL=postgresql://user:pass@host:5432/db" \\
  --set-env-vars "PORT=8080" \\
  --set-env-vars "JWT_SECRET=my-secret"

# Or via docker-compose.yml
services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      DATABASE_URL: postgresql://demo:demo@db:5432/demo
      PORT: "8080"
      JWT_SECRET: dev-secret""";

        return Section.create().className("doc-section")
            .child(h2("Deployment Examples"))
            .child(p("Here are examples of setting environment variables in common "
                + "deployment targets:"))
            .child(h3("AWS Lambda"))
            .child(CodeBlock.create(lambdaCode, "yaml"))
            .child(h3("Cloud Run / Docker Compose"))
            .child(CodeBlock.create(cloudRunCode, "bash"))
            .child(Callout.pitfall("Secrets management",
                p("Never commit secrets like JWT_SECRET or DATABASE_URL passwords to "
                    + "source control. Use your platform's secrets manager -- AWS Secrets "
                    + "Manager, Google Secret Manager, or environment-specific .env files "
                    + "excluded from version control.")))
            .build();
    }
}
