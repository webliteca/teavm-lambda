package ca.weblite.teavmlambda.docs;

import ca.weblite.teavmreact.core.ReactDOM;
import ca.weblite.teavmlambda.docs.pages.HomePage;
import ca.weblite.teavmlambda.docs.pages.CreditsPage;
import ca.weblite.teavmlambda.docs.pages.learn.*;
import ca.weblite.teavmlambda.docs.pages.reference.*;
import org.teavm.jso.dom.html.HTMLDocument;

public class App {

    private static final Route[] ROUTES = {
        // Homepage (full width, no sidebar)
        new Route("", HomePage::render, true),

        // Learn section
        new Route("learn/quick-start", QuickStartPage::render),
        new Route("learn/installation", InstallationPage::render),
        new Route("learn/routing", RoutingPage::render),
        new Route("learn/dependency-injection", DependencyInjectionPage::render),
        new Route("learn/request-response", RequestResponsePage::render),
        new Route("learn/middleware", MiddlewarePage::render),
        new Route("learn/database", DatabasePage::render),
        new Route("learn/cloud-services", CloudServicesPage::render),
        new Route("learn/security", SecurityPage::render),
        new Route("learn/validation", ValidationPage::render),
        new Route("learn/deployment", DeploymentPage::render),

        // Reference section
        new Route("reference/annotations", AnnotationsPage::render),
        new Route("reference/core-api", CoreApiPage::render),
        new Route("reference/kotlin-dsl", KotlinDslPage::render),
        new Route("reference/environment", EnvironmentPage::render),

        // Credits
        new Route("credits", CreditsPage::render),
    };

    public static void main(String[] args) {
        var root = ReactDOM.createRoot(HTMLDocument.current().getElementById("root"));
        root.render(Router.create(ROUTES));
    }
}
