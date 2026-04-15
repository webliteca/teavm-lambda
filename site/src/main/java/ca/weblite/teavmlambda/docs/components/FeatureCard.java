package ca.weblite.teavmlambda.docs.components;

import ca.weblite.teavmreact.core.ReactElement;
import ca.weblite.teavmreact.html.DomBuilder.*;

import static ca.weblite.teavmreact.html.Html.*;

public class FeatureCard {

    public static ReactElement create(String title, String description) {
        return Div.create().className("feature-card")
            .child(H3.create().text(title))
            .child(P.create().text(description))
            .build();
    }
}
