package io.teavmlambda.processor;

import io.teavmlambda.core.annotation.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@SupportedAnnotationTypes("io.teavmlambda.core.annotation.Path")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class RouteProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> pathElements = roundEnv.getElementsAnnotatedWith(Path.class);

        List<ResourceClass> resources = new ArrayList<>();
        for (Element element : pathElements) {
            if (element.getKind() != ElementKind.CLASS) {
                continue;
            }
            TypeElement typeElement = (TypeElement) element;
            ResourceClass resource = processResourceClass(typeElement);
            if (resource != null) {
                resources.add(resource);
            }
        }

        if (!resources.isEmpty()) {
            generateRouter(resources);
        }

        return true;
    }

    private ResourceClass processResourceClass(TypeElement typeElement) {
        Path pathAnnotation = typeElement.getAnnotation(Path.class);
        String basePath = normalizePath(pathAnnotation.value());
        String qualifiedName = typeElement.getQualifiedName().toString();
        String simpleName = typeElement.getSimpleName().toString();

        List<RouteMethod> methods = new ArrayList<>();
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (enclosed.getKind() != ElementKind.METHOD) {
                continue;
            }
            ExecutableElement method = (ExecutableElement) enclosed;
            String httpMethod = extractHttpMethod(method);
            if (httpMethod == null) {
                continue;
            }

            Path methodPath = method.getAnnotation(Path.class);
            String fullPath = basePath;
            if (methodPath != null) {
                fullPath = basePath + normalizePath(methodPath.value());
            }

            List<MethodParam> params = extractParams(method);
            methods.add(new RouteMethod(httpMethod, fullPath, method.getSimpleName().toString(), params));
        }

        if (methods.isEmpty()) {
            return null;
        }

        return new ResourceClass(qualifiedName, simpleName, methods);
    }

    private String extractHttpMethod(ExecutableElement method) {
        if (method.getAnnotation(GET.class) != null) return "GET";
        if (method.getAnnotation(POST.class) != null) return "POST";
        if (method.getAnnotation(PUT.class) != null) return "PUT";
        if (method.getAnnotation(DELETE.class) != null) return "DELETE";
        return null;
    }

    private List<MethodParam> extractParams(ExecutableElement method) {
        List<MethodParam> params = new ArrayList<>();
        for (VariableElement param : method.getParameters()) {
            TypeMirror type = param.asType();
            String typeName = type.toString();

            PathParam pathParam = param.getAnnotation(PathParam.class);
            if (pathParam != null) {
                params.add(new MethodParam(MethodParam.Kind.PATH, pathParam.value(), typeName));
                continue;
            }

            QueryParam queryParam = param.getAnnotation(QueryParam.class);
            if (queryParam != null) {
                params.add(new MethodParam(MethodParam.Kind.QUERY, queryParam.value(), typeName));
                continue;
            }

            if (param.getAnnotation(Body.class) != null) {
                params.add(new MethodParam(MethodParam.Kind.BODY, null, typeName));
                continue;
            }

            // Unannotated Request parameter
            if (typeName.equals("io.teavmlambda.core.Request")) {
                params.add(new MethodParam(MethodParam.Kind.REQUEST, null, typeName));
            }
        }
        return params;
    }

    private void generateRouter(List<ResourceClass> resources) {
        String packageName = "io.teavmlambda.generated";
        String className = "GeneratedRouter";

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + className);
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.println("package " + packageName + ";");
                out.println();
                out.println("import io.teavmlambda.core.Request;");
                out.println("import io.teavmlambda.core.Response;");
                out.println("import io.teavmlambda.core.Router;");
                out.println("import java.util.HashMap;");
                out.println("import java.util.Map;");
                out.println();
                out.println("public final class GeneratedRouter implements Router {");
                out.println();

                // Fields for resource instances
                for (ResourceClass resource : resources) {
                    String fieldName = fieldNameFor(resource.simpleName);
                    out.println("    private final " + resource.qualifiedName + " " + fieldName + ";");
                }
                out.println();

                // Constructor taking resource instances
                out.print("    public GeneratedRouter(");
                for (int i = 0; i < resources.size(); i++) {
                    ResourceClass resource = resources.get(i);
                    String fieldName = fieldNameFor(resource.simpleName);
                    if (i > 0) out.print(", ");
                    out.print(resource.qualifiedName + " " + fieldName);
                }
                out.println(") {");
                for (ResourceClass resource : resources) {
                    String fieldName = fieldNameFor(resource.simpleName);
                    out.println("        this." + fieldName + " = " + fieldName + ";");
                }
                out.println("    }");
                out.println();

                // route() method
                out.println("    @Override");
                out.println("    public Response route(Request request) {");
                out.println("        String method = request.getMethod();");
                out.println("        String path = request.getPath();");
                out.println("        if (path.endsWith(\"/\") && path.length() > 1) {");
                out.println("            path = path.substring(0, path.length() - 1);");
                out.println("        }");
                out.println("        String[] segments = path.split(\"/\", -1);");
                out.println();

                for (ResourceClass resource : resources) {
                    String fieldName = fieldNameFor(resource.simpleName);
                    for (RouteMethod route : resource.methods) {
                        generateRouteMatch(out, route, fieldName);
                    }
                }

                out.println("        return Response.status(404).body(\"Not Found\");");
                out.println("    }");
                out.println("}");
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate router: " + e.getMessage());
        }
    }

    private void generateRouteMatch(PrintWriter out, RouteMethod route, String fieldName) {
        String[] templateSegments = route.path.split("/", -1);
        // Remove empty first segment from leading "/"
        List<String> segments = new ArrayList<>();
        for (String s : templateSegments) {
            if (!s.isEmpty()) segments.add(s);
        }

        out.println("        // " + route.httpMethod + " " + route.path);
        out.print("        if (\"" + route.httpMethod + "\".equals(method)");
        out.print(" && segments.length == " + (segments.size() + 1));

        // Static segment checks
        for (int i = 0; i < segments.size(); i++) {
            String seg = segments.get(i);
            if (!seg.startsWith("{")) {
                out.print(" && \"" + seg + "\".equals(segments[" + (i + 1) + "])");
            }
        }
        out.println(") {");

        // Extract path params and build pathParams map if needed
        boolean hasPathParams = route.params.stream().anyMatch(p -> p.kind == MethodParam.Kind.PATH);
        if (hasPathParams) {
            out.println("            Map<String, String> pathParams = new HashMap<>();");
            for (int i = 0; i < segments.size(); i++) {
                String seg = segments.get(i);
                if (seg.startsWith("{") && seg.endsWith("}")) {
                    String paramName = seg.substring(1, seg.length() - 1);
                    out.println("            pathParams.put(\"" + paramName + "\", segments[" + (i + 1) + "]);");
                }
            }
        }

        // Build method call arguments
        StringBuilder args = new StringBuilder();
        for (int i = 0; i < route.params.size(); i++) {
            if (i > 0) args.append(", ");
            MethodParam param = route.params.get(i);
            switch (param.kind) {
                case PATH:
                    args.append("pathParams.get(\"").append(param.name).append("\")");
                    break;
                case QUERY:
                    args.append("request.getQueryParams().get(\"").append(param.name).append("\")");
                    break;
                case BODY:
                    args.append("request.getBody()");
                    break;
                case REQUEST:
                    args.append("request");
                    break;
            }
        }

        out.println("            return " + fieldName + "." + route.methodName + "(" + args + ");");
        out.println("        }");
    }

    private String normalizePath(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        if (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    private String fieldNameFor(String simpleName) {
        return Character.toLowerCase(simpleName.charAt(0)) + simpleName.substring(1);
    }

    // Internal model classes

    private record ResourceClass(String qualifiedName, String simpleName, List<RouteMethod> methods) {}

    private record RouteMethod(String httpMethod, String path, String methodName, List<MethodParam> params) {}

    private record MethodParam(Kind kind, String name, String typeName) {
        enum Kind { PATH, QUERY, BODY, REQUEST }
    }
}
