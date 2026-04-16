package ca.weblite.teavmlambda.docs.pages;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.Div;
import ca.weblite.teavmreact.html.DomBuilder.Section;
import org.teavm.jso.JSObject;
import static ca.weblite.teavmreact.html.Html.*;
import ca.weblite.teavmlambda.docs.El;

public class CreditsPage {

    public static ReactElement render(JSObject props) {
        return Div.create().className("doc-page")
            .child(h1("Credits"))
            .child(p("teavm-lambda stands on the shoulders of remarkable open source projects "
                + "and the people behind them."))

            .child(teavmSection())
            .child(reactSection())
            .child(creatorSection())
            .child(sponsorSection())
            .build();
    }

    private static ReactElement teavmSection() {
        return Section.create().className("doc-section")
            .child(h2("TeaVM"))
            .child(p("teavm-lambda would not exist without "
                + "the incredible work of "))
            .child(Div.create().className("credit-card")
                .child(h3("Alexey Andreev"))
                .child(p(
                    "Alexey created and maintains TeaVM, the ahead-of-time compiler that "
                    + "makes it possible to compile Java to JavaScript. TeaVM is the foundation "
                    + "that enables teavm-lambda's ~100ms cold starts on AWS Lambda and "
                    + "its Write Once, Run Anywhere architecture."))
                .child(Div.create().className("credit-links")
                    .child(a("GitHub").href("https://github.com/konsoletyper")
                        .target("_blank").className("btn btn-secondary").build())
                    .child(a("TeaVM").href("https://teavm.org")
                        .target("_blank").className("btn btn-secondary").build())
                    .child(a("Sponsor Alexey").href("https://github.com/sponsors/konsoletyper")
                        .target("_blank").className("btn btn-primary").build())))
            .build();
    }

    private static ReactElement reactSection() {
        return Section.create().className("doc-section")
            .child(h2("React"))
            .child(p("The documentation site you are viewing right now is built with "
                + "teavm-react, which provides Java and Kotlin bindings to React 18. "
                + "React's component model, virtual DOM, and hooks API make it possible "
                + "to build rich, interactive documentation entirely in Java."))
            .child(Div.create().className("credit-links")
                .child(a("React").href("https://react.dev")
                    .target("_blank").className("btn btn-secondary").build())
                .child(a("teavm-react").href("https://github.com/webliteca/teavm-react")
                    .target("_blank").className("btn btn-secondary").build()))
            .build();
    }

    private static ReactElement creatorSection() {
        return Section.create().className("doc-section")
            .child(h2("Created By"))
            .child(Div.create().className("credit-card")
                .child(h3("Steve Hannah"))
                .child(p(
                    "teavm-lambda and teavm-react were created by Steve Hannah. "
                    + "Both projects are open source under the MIT License."))
                .child(Div.create().className("credit-links")
                    .child(a("teavm-lambda").href("https://github.com/webliteca/teavm-lambda")
                        .target("_blank").className("btn btn-secondary").build())
                    .child(a("teavm-react").href("https://github.com/webliteca/teavm-react")
                        .target("_blank").className("btn btn-secondary").build())))
            .build();
    }

    private static ReactElement sponsorSection() {
        return Section.create().className("doc-section")
            .child(h2("Support This Project"))
            .child(p("The best way to support teavm-lambda is to support the project "
                + "it depends on most. TeaVM is the engine that makes everything possible, "
                + "and Alexey Andreev maintains it as an open source labor of love."))
            .child(Div.create().className("sponsor-cta")
                .child(p(
                    "If teavm-lambda has saved you time or helped your project, "
                    + "please consider becoming a sponsor:"))
                .child(a("Sponsor Alexey Andreev on GitHub")
                    .href("https://github.com/sponsors/konsoletyper")
                    .target("_blank")
                    .className("btn btn-primary sponsor-cta-btn")
                    .build()))
            .build();
    }
}
