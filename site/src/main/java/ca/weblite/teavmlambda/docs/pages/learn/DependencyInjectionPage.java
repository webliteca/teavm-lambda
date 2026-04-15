package ca.weblite.teavmlambda.docs.pages.learn;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.Div;
import ca.weblite.teavmreact.html.DomBuilder.Section;
import org.teavm.jso.JSObject;
import static ca.weblite.teavmreact.html.Html.*;
import ca.weblite.teavmlambda.docs.El;
import ca.weblite.teavmlambda.docs.components.CodeBlock;
import ca.weblite.teavmlambda.docs.components.CodeTabs;
import ca.weblite.teavmlambda.docs.components.Callout;
import ca.weblite.teavmlambda.docs.components.FeatureCard;
import ca.weblite.teavmlambda.docs.components.ProjectInitializer;

public class DependencyInjectionPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Dependency Injection"))
            .child(p("teavm-lambda provides compile-time dependency injection powered by "
                + "the annotation processor. There is no runtime container, no reflection, "
                + "and no classpath scanning. The GeneratedContainer class is created at "
                + "build time with all dependencies statically wired."))
            .child(sectionComponentAnnotations())
            .child(sectionSingleton())
            .child(sectionConstructorInjection())
            .child(sectionFullExample())
            .child(sectionHowItWorks())
            .build();
    }

    private static ReactElement sectionComponentAnnotations() {
        String javaCode = """
                // @Component - general-purpose managed bean
                @Component
                public class EmailService {
                    public void send(String to, String subject, String body) {
                        // send email
                    }
                }

                // @Service - business logic layer
                @Service
                public class OrderService {
                    // ...
                }

                // @Repository - data access layer
                @Repository
                public class UserRepository {
                    // ...
                }""";

        String kotlinCode = """
                // @Component - general-purpose managed bean
                @Component
                class EmailService {
                    fun send(to: String, subject: String, body: String) {
                        // send email
                    }
                }

                // @Service - business logic layer
                @Service
                class OrderService {
                    // ...
                }

                // @Repository - data access layer
                @Repository
                class UserRepository {
                    // ...
                }""";

        return Section.create().className("doc-section")
            .child(h2("Component Annotations"))
            .child(p("Three annotations mark classes for dependency injection. All three "
                + "behave identically at the framework level -- the distinction is purely "
                + "organizational to help convey the role of each class in your architecture."))
            .child(ul(
                li("@Component - a general-purpose managed bean"),
                li("@Service - a class encapsulating business logic"),
                li("@Repository - a class responsible for data access")))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(p("The annotation processor treats all three annotations the same way. "
                + "Any class annotated with @Component, @Service, or @Repository will be "
                + "registered in the GeneratedContainer and available for injection."))
            .build();
    }

    private static ReactElement sectionSingleton() {
        String javaCode = """
                @Component
                @Singleton
                public class ConfigService {
                    private final Map<String, String> config;

                    public ConfigService() {
                        this.config = new HashMap<>();
                        config.put("app.name", "my-app");
                        config.put("app.version", "1.0.0");
                    }

                    public String get(String key) {
                        return config.get(key);
                    }
                }""";

        String kotlinCode = """
                @Component
                @Singleton
                class ConfigService {
                    private val config = mapOf(
                        "app.name" to "my-app",
                        "app.version" to "1.0.0"
                    )

                    fun get(key: String): String? = config[key]
                }""";

        return Section.create().className("doc-section")
            .child(h2("@Singleton"))
            .child(p("By default, the container creates a new instance each time a "
                + "dependency is requested. Add @Singleton to ensure only one instance "
                + "is created and shared across all injection points. This is useful for "
                + "stateful services like configuration, caching, or connection pools."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(Callout.note("Singleton Lifecycle",
                p("Singleton instances are created once when the GeneratedContainer "
                    + "is constructed and reused for the lifetime of the application. "
                    + "The creation order is determined by the dependency graph.")))
            .build();
    }

    private static ReactElement sectionConstructorInjection() {
        String javaCode = """
                @Service
                @Singleton
                public class NotificationService {
                    private final EmailService emailService;
                    private final ConfigService configService;

                    @Inject
                    public NotificationService(EmailService emailService,
                                               ConfigService configService) {
                        this.emailService = emailService;
                        this.configService = configService;
                    }

                    public void notifyUser(String email, String message) {
                        String appName = configService.get("app.name");
                        emailService.send(email, appName + " Notification", message);
                    }
                }""";

        String kotlinCode = """
                @Service
                @Singleton
                class NotificationService @Inject constructor(
                    private val emailService: EmailService,
                    private val configService: ConfigService
                ) {
                    fun notifyUser(email: String, message: String) {
                        val appName = configService.get("app.name")
                        emailService.send(email, "$appName Notification", message)
                    }
                }""";

        return Section.create().className("doc-section")
            .child(h2("Constructor Injection with @Inject"))
            .child(p("Use @Inject on a constructor to declare dependencies. The annotation "
                + "processor resolves the dependency graph at compile time and generates "
                + "code that passes the correct instances when constructing each component."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .child(p("Constructor injection is the only supported injection style. Field "
                + "injection and setter injection are not available. This restriction "
                + "makes dependencies explicit and ensures they are always satisfied."))
            .build();
    }

    private static ReactElement sectionFullExample() {
        String javaCode = """
                @Repository
                @Singleton
                public class UserRepository {
                    private final Database db;

                    @Inject
                    public UserRepository(Database db) {
                        this.db = db;
                    }

                    public String findById(String id) {
                        DbResult result = db.query(
                            "SELECT * FROM users WHERE id = $1", id);
                        if (result.getRowCount() == 0) return null;
                        DbRow row = result.getRows().get(0);
                        return new JsonBuilder()
                                .put("id", row.getString("id"))
                                .put("name", row.getString("name"))
                                .put("email", row.getString("email"))
                                .build();
                    }
                }

                @Service
                @Singleton
                public class UserService {
                    private final UserRepository userRepository;

                    @Inject
                    public UserService(UserRepository userRepository) {
                        this.userRepository = userRepository;
                    }

                    public String getUser(String id) {
                        return userRepository.findById(id);
                    }
                }

                @Path("/users")
                @Component
                @Singleton
                public class UserResource {
                    private final UserService userService;

                    @Inject
                    public UserResource(UserService userService) {
                        this.userService = userService;
                    }

                    @GET
                    @Path("/{id}")
                    public Response getUser(@PathParam("id") String id) {
                        String user = userService.getUser(id);
                        if (user == null) {
                            return Response.status(404)
                                    .body("{\\"error\\":\\"User not found\\"}")
                                    .header("Content-Type", "application/json");
                        }
                        return Response.ok(user)
                                .header("Content-Type", "application/json");
                    }
                }""";

        String kotlinCode = """
                @Repository
                @Singleton
                class UserRepository @Inject constructor(
                    private val db: Database
                ) {
                    fun findById(id: String): String? {
                        val result = db.query(
                            "SELECT * FROM users WHERE id = $1", id)
                        if (result.rowCount == 0) return null
                        val row = result.rows[0]
                        return json {
                            "id" to row.getString("id")
                            "name" to row.getString("name")
                            "email" to row.getString("email")
                        }
                    }
                }

                @Service
                @Singleton
                class UserService @Inject constructor(
                    private val userRepository: UserRepository
                ) {
                    fun getUser(id: String): String? =
                        userRepository.findById(id)
                }

                @Path("/users")
                @Component
                @Singleton
                class UserResource @Inject constructor(
                    private val userService: UserService
                ) {
                    @GET
                    @Path("/{id}")
                    fun getUser(@PathParam("id") id: String): Response {
                        val user = userService.getUser(id)
                            ?: return Response.status(404)
                                .body(json { "error" to "User not found" })
                                .header("Content-Type", "application/json")
                        return Response.ok(user)
                            .header("Content-Type", "application/json")
                    }
                }""";

        return Section.create().className("doc-section")
            .child(h2("Full Example: Three-Layer Architecture"))
            .child(p("Here is a complete example showing the common three-layer pattern: "
                + "a Resource (controller) depends on a Service (business logic), which "
                + "depends on a Repository (data access). The annotation processor "
                + "resolves the full dependency chain at compile time."))
            .child(CodeTabs.create(javaCode, kotlinCode))
            .build();
    }

    private static ReactElement sectionHowItWorks() {
        String generatedCode = """
                // This code is generated by the annotation processor.
                // You never write this by hand.
                public class GeneratedContainer implements Container {

                    private final Database database;
                    private final UserRepository userRepository;
                    private final UserService userService;
                    private final UserResource userResource;

                    public GeneratedContainer() {
                        this.database = DatabaseFactory.create();
                        this.userRepository = new UserRepository(database);
                        this.userService = new UserService(userRepository);
                        this.userResource = new UserResource(userService);
                    }

                    // ...
                }""";

        return Section.create().className("doc-section")
            .child(h2("How It Works"))
            .child(p("The annotation processor reads the @Component, @Service, @Repository, "
                + "@Singleton, and @Inject annotations from your source code. It builds a "
                + "dependency graph, performs topological sorting, and generates the "
                + "GeneratedContainer class that constructs every component in the "
                + "correct order."))
            .child(CodeBlock.create(generatedCode, "java"))
            .child(p("Because all wiring happens at compile time, there is no startup "
                + "cost for dependency resolution. The generated code is plain constructor "
                + "calls with no reflection or dynamic proxies."))
            .child(Callout.pitfall("Compile-Time Only",
                p("DI is resolved at compile time -- there is no runtime registration. "
                    + "You cannot add or remove components dynamically. If the annotation "
                    + "processor cannot resolve a dependency (for example, a missing class "
                    + "or a circular dependency), you will get a compilation error, not "
                    + "a runtime exception.")))
            .build();
    }
}
