package ca.weblite.teavmlambda.processor;

import ca.weblite.teavmlambda.api.annotation.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Annotation processor that generates {@code GeneratedContainer} at compile time.
 * <p>
 * Scans for classes annotated with {@code @Component}, {@code @Service}, or
 * {@code @Repository}. For each, finds the {@code @Inject}-annotated constructor
 * (or the default constructor), resolves dependencies, topologically sorts them,
 * and generates registration code.
 */
@SupportedAnnotationTypes({
        "ca.weblite.teavmlambda.api.annotation.Component",
        "ca.weblite.teavmlambda.api.annotation.Service",
        "ca.weblite.teavmlambda.api.annotation.Repository"
})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class ContainerProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        List<ManagedComponent> components = new ArrayList<>();

        collectComponents(roundEnv, Component.class, components);
        collectComponents(roundEnv, Service.class, components);
        collectComponents(roundEnv, Repository.class, components);

        if (components.isEmpty()) {
            return true;
        }

        // Topological sort by dependencies
        List<ManagedComponent> sorted = topologicalSort(components);

        generateContainer(sorted);

        return true;
    }

    private <A extends java.lang.annotation.Annotation> void collectComponents(
            RoundEnvironment roundEnv, Class<A> annotationType, List<ManagedComponent> components) {

        for (Element element : roundEnv.getElementsAnnotatedWith(annotationType)) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }
            TypeElement typeElement = (TypeElement) element;
            ManagedComponent component = processComponent(typeElement);
            if (component != null) {
                components.add(component);
            }
        }
    }

    private ManagedComponent processComponent(TypeElement typeElement) {
        String qualifiedName = typeElement.getQualifiedName().toString();
        boolean isSingleton = typeElement.getAnnotation(Singleton.class) != null;

        // Find @Inject constructor, or fall back to default constructor
        ExecutableElement injectConstructor = null;
        ExecutableElement defaultConstructor = null;

        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.CONSTRUCTOR) {
                continue;
            }
            ExecutableElement ctor = (ExecutableElement) enclosed;
            if (ctor.getAnnotation(Inject.class) != null) {
                injectConstructor = ctor;
                break;
            }
            if (ctor.getParameters().isEmpty()) {
                defaultConstructor = ctor;
            }
        }

        ExecutableElement constructor = injectConstructor != null ? injectConstructor : defaultConstructor;
        if (constructor == null) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "No @Inject constructor or default constructor found for " + qualifiedName,
                    typeElement);
            return null;
        }

        // Collect constructor parameter types as dependencies
        List<String> dependencies = new ArrayList<>();
        for (VariableElement param : constructor.getParameters()) {
            TypeMirror paramType = param.asType();
            // Resolve to the declared type's qualified name
            String depType = getQualifiedName(paramType);
            dependencies.add(depType);
        }

        // Collect interfaces implemented by this class (for registration by interface)
        List<String> interfaces = new ArrayList<>();
        for (TypeMirror iface : typeElement.getInterfaces()) {
            interfaces.add(getQualifiedName(iface));
        }

        return new ManagedComponent(qualifiedName, isSingleton, dependencies, interfaces);
    }

    private String getQualifiedName(TypeMirror typeMirror) {
        if (typeMirror instanceof DeclaredType declaredType) {
            Element element = declaredType.asElement();
            if (element instanceof TypeElement typeElement) {
                return typeElement.getQualifiedName().toString();
            }
        }
        return typeMirror.toString();
    }

    private List<ManagedComponent> topologicalSort(List<ManagedComponent> components) {
        // Build a map for quick lookup
        Map<String, ManagedComponent> byName = new LinkedHashMap<>();
        for (ManagedComponent c : components) {
            byName.put(c.qualifiedName, c);
        }

        List<ManagedComponent> sorted = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> visiting = new HashSet<>();

        for (ManagedComponent c : components) {
            visit(c, byName, visited, visiting, sorted);
        }

        return sorted;
    }

    private void visit(ManagedComponent component, Map<String, ManagedComponent> byName,
                       Set<String> visited, Set<String> visiting, List<ManagedComponent> sorted) {
        if (visited.contains(component.qualifiedName)) {
            return;
        }
        if (visiting.contains(component.qualifiedName)) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Circular dependency detected involving " + component.qualifiedName);
            return;
        }

        visiting.add(component.qualifiedName);

        for (String dep : component.dependencies) {
            // Find if a managed component provides this type (either directly or via interface)
            ManagedComponent depComponent = byName.get(dep);
            if (depComponent == null) {
                // Check if any component implements this interface
                for (ManagedComponent candidate : byName.values()) {
                    if (candidate.interfaces.contains(dep)) {
                        depComponent = candidate;
                        break;
                    }
                }
            }
            if (depComponent != null) {
                visit(depComponent, byName, visited, visiting, sorted);
            }
            // If not found, it's an external dependency (e.g. Database) — will be pre-registered
        }

        visiting.remove(component.qualifiedName);
        visited.add(component.qualifiedName);
        sorted.add(component);
    }

    private void generateContainer(List<ManagedComponent> components) {
        String packageName = "ca.weblite.teavmlambda.generated";
        String className = "GeneratedContainer";

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + className);
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.println("package " + packageName + ";");
                out.println();
                out.println("import ca.weblite.teavmlambda.api.Container;");
                out.println();
                out.println("/**");
                out.println(" * Auto-generated dependency injection container.");
                out.println(" * Registers all @Component/@Service/@Repository classes discovered at compile time.");
                out.println(" */");
                out.println("public final class GeneratedContainer extends Container {");
                out.println();
                out.println("    public GeneratedContainer() {");
                out.println("        wireComponents();");
                out.println("    }");
                out.println();
                out.println("    private void wireComponents() {");

                for (ManagedComponent component : components) {
                    String registerMethod = component.isSingleton ? "registerSingleton" : "register";

                    // Build the constructor call with container.get() for each dependency
                    StringBuilder ctorArgs = new StringBuilder();
                    for (int i = 0; i < component.dependencies.size(); i++) {
                        if (i > 0) ctorArgs.append(", ");
                        ctorArgs.append("get(").append(component.dependencies.get(i)).append(".class)");
                    }

                    // Register by class name
                    out.println("        " + registerMethod + "(" + component.qualifiedName + ".class, "
                            + "() -> new " + component.qualifiedName + "(" + ctorArgs + "));");

                    // Also register by each implemented interface
                    for (String iface : component.interfaces) {
                        out.println("        " + registerMethod + "(" + iface + ".class, "
                                + "() -> get(" + component.qualifiedName + ".class));");
                    }
                }

                out.println("    }");
                out.println("}");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate container: " + e.getMessage());
        }
    }

    private record ManagedComponent(String qualifiedName, boolean isSingleton,
                                    List<String> dependencies, List<String> interfaces) {}
}
