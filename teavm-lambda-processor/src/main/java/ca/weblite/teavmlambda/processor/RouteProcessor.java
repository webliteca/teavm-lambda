package ca.weblite.teavmlambda.processor;

import ca.weblite.teavmlambda.api.annotation.*;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

@SupportedAnnotationTypes("ca.weblite.teavmlambda.api.annotation.Path")
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
            String openApiJson = buildOpenApiJson(resources);
            writeOpenApiResource(openApiJson);
        }

        return true;
    }

    private ResourceClass processResourceClass(TypeElement typeElement) {
        Path pathAnnotation = typeElement.getAnnotation(Path.class);
        String basePath = normalizePath(pathAnnotation.value());
        String qualifiedName = typeElement.getQualifiedName().toString();
        String simpleName = typeElement.getSimpleName().toString();

        ApiTag apiTag = typeElement.getAnnotation(ApiTag.class);
        String tagName = apiTag != null ? apiTag.value() : simpleName;
        String tagDescription = apiTag != null ? apiTag.description() : "";

        ApiInfo apiInfo = typeElement.getAnnotation(ApiInfo.class);

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

            ApiOperation apiOp = method.getAnnotation(ApiOperation.class);
            String summary = apiOp != null ? apiOp.summary() : "";
            String description = apiOp != null ? apiOp.description() : "";

            List<ApiResponseInfo> responseInfos = new ArrayList<>();
            ApiResponse[] responses = method.getAnnotationsByType(ApiResponse.class);
            for (ApiResponse r : responses) {
                responseInfos.add(new ApiResponseInfo(r.code(), r.description(), r.mediaType()));
            }

            methods.add(new RouteMethod(httpMethod, fullPath, method.getSimpleName().toString(),
                    params, summary, description, responseInfos));
        }

        if (methods.isEmpty()) {
            return null;
        }

        return new ResourceClass(qualifiedName, simpleName, methods, tagName, tagDescription, apiInfo);
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
            if (typeName.equals("ca.weblite.teavmlambda.api.Request")) {
                params.add(new MethodParam(MethodParam.Kind.REQUEST, null, typeName));
            }
        }
        return params;
    }

    private void generateRouter(List<ResourceClass> resources) {
        String packageName = "ca.weblite.teavmlambda.generated";
        String className = "GeneratedRouter";

        try {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(packageName + "." + className);
            try (PrintWriter out = new PrintWriter(file.openWriter())) {
                out.println("package " + packageName + ";");
                out.println();
                out.println("import ca.weblite.teavmlambda.api.Request;");
                out.println("import ca.weblite.teavmlambda.api.Response;");
                out.println("import ca.weblite.teavmlambda.api.Router;");
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

                // OpenAPI spec loaded at runtime from resources/
                out.println("    private static final String OPENAPI_SPEC;");
                out.println();
                out.println("    static {");
                out.println("        String spec = ca.weblite.teavmlambda.api.Resources.loadText(\"/openapi.json\");");
                out.println("        OPENAPI_SPEC = spec != null ? spec : \"{}\";");
                out.println("    }");
                out.println();

                // Swagger UI HTML constant
                out.println("    private static final String SWAGGER_UI_HTML = \"<!DOCTYPE html>\"");
                out.println("        + \"<html lang=\\\"en\\\"><head><meta charset=\\\"UTF-8\\\">\"");
                out.println("        + \"<title>API Documentation</title>\"");
                out.println("        + \"<link rel=\\\"stylesheet\\\" href=\\\"https://unpkg.com/swagger-ui-dist@5/swagger-ui.css\\\">\"");
                out.println("        + \"</head><body>\"");
                out.println("        + \"<div id=\\\"swagger-ui\\\"></div>\"");
                out.println("        + \"<script src=\\\"https://unpkg.com/swagger-ui-dist@5/swagger-ui-bundle.js\\\"></script>\"");
                out.println("        + \"<script>SwaggerUIBundle({url:'/openapi.json',dom_id:'#swagger-ui'})</script>\"");
                out.println("        + \"</body></html>\";");
                out.println();

                // Constructor taking a Container (DI)
                out.println("    public GeneratedRouter(ca.weblite.teavmlambda.api.Container container) {");
                for (ResourceClass resource : resources) {
                    String fieldName = fieldNameFor(resource.simpleName);
                    out.println("        this." + fieldName + " = container.get(" + resource.qualifiedName + ".class);");
                }
                out.println("    }");
                out.println();

                // Backwards-compatible constructor taking resource instances directly
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
                out.println();

                // Built-in OpenAPI routes
                out.println("        // Built-in OpenAPI routes");
                out.println("        if (\"GET\".equals(method) && \"/openapi.json\".equals(path)) {");
                out.println("            return Response.ok(OPENAPI_SPEC)");
                out.println("                .header(\"Content-Type\", \"application/json\")");
                out.println("                .header(\"Access-Control-Allow-Origin\", \"*\");");
                out.println("        }");
                out.println("        if (\"GET\".equals(method) && \"/swagger-ui\".equals(path)) {");
                out.println("            return Response.ok(SWAGGER_UI_HTML)");
                out.println("                .header(\"Content-Type\", \"text/html\");");
                out.println("        }");
                out.println();

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

    private String buildOpenApiJson(List<ResourceClass> resources) {
        // Find @ApiInfo if any resource has it
        String title = "API";
        String version = "1.0.0";
        String infoDescription = "";
        for (ResourceClass resource : resources) {
            if (resource.apiInfo != null) {
                title = resource.apiInfo.title();
                version = resource.apiInfo.version();
                infoDescription = resource.apiInfo.description();
                break;
            }
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"openapi\": \"3.0.3\",\n");
        json.append("  \"info\": {\n");
        json.append("    \"title\": ").append(jsonString(title)).append(",\n");
        json.append("    \"version\": ").append(jsonString(version));
        if (!infoDescription.isEmpty()) {
            json.append(",\n    \"description\": ").append(jsonString(infoDescription));
        }
        json.append("\n  },\n");

        // Tags
        json.append("  \"tags\": [\n");
        for (int i = 0; i < resources.size(); i++) {
            ResourceClass resource = resources.get(i);
            json.append("    {\n");
            json.append("      \"name\": ").append(jsonString(resource.tagName));
            if (!resource.tagDescription.isEmpty()) {
                json.append(",\n      \"description\": ").append(jsonString(resource.tagDescription));
            }
            json.append("\n    }");
            if (i < resources.size() - 1) json.append(",");
            json.append("\n");
        }
        json.append("  ],\n");

        // Paths — group routes by path
        Map<String, List<RouteWithTag>> pathMap = new LinkedHashMap<>();
        for (ResourceClass resource : resources) {
            for (RouteMethod route : resource.methods) {
                String openApiPath = route.path.replaceAll("\\{([^}]+)\\}", "{$1}");
                pathMap.computeIfAbsent(openApiPath, k -> new ArrayList<>())
                        .add(new RouteWithTag(route, resource.tagName));
            }
        }

        json.append("  \"paths\": {\n");
        int pathIndex = 0;
        for (Map.Entry<String, List<RouteWithTag>> entry : pathMap.entrySet()) {
            String path = entry.getKey();
            List<RouteWithTag> routes = entry.getValue();

            json.append("    ").append(jsonString(path)).append(": {\n");
            for (int r = 0; r < routes.size(); r++) {
                RouteWithTag rwt = routes.get(r);
                RouteMethod route = rwt.route;
                String method = route.httpMethod.toLowerCase();

                json.append("      ").append(jsonString(method)).append(": {\n");

                // Operation ID
                json.append("        \"operationId\": ").append(jsonString(route.methodName)).append(",\n");

                // Tags
                json.append("        \"tags\": [").append(jsonString(rwt.tagName)).append("],\n");

                // Summary and description
                if (!route.summary.isEmpty()) {
                    json.append("        \"summary\": ").append(jsonString(route.summary)).append(",\n");
                }
                if (!route.description.isEmpty()) {
                    json.append("        \"description\": ").append(jsonString(route.description)).append(",\n");
                }

                // Parameters
                List<MethodParam> paramList = new ArrayList<>();
                for (MethodParam p : route.params) {
                    if (p.kind == MethodParam.Kind.PATH || p.kind == MethodParam.Kind.QUERY) {
                        paramList.add(p);
                    }
                }
                if (!paramList.isEmpty()) {
                    json.append("        \"parameters\": [\n");
                    for (int p = 0; p < paramList.size(); p++) {
                        MethodParam param = paramList.get(p);
                        String in = param.kind == MethodParam.Kind.PATH ? "path" : "query";
                        json.append("          {\n");
                        json.append("            \"name\": ").append(jsonString(param.name)).append(",\n");
                        json.append("            \"in\": ").append(jsonString(in)).append(",\n");
                        if (param.kind == MethodParam.Kind.PATH) {
                            json.append("            \"required\": true,\n");
                        }
                        json.append("            \"schema\": { \"type\": \"string\" }\n");
                        json.append("          }");
                        if (p < paramList.size() - 1) json.append(",");
                        json.append("\n");
                    }
                    json.append("        ],\n");
                }

                // Request body
                boolean hasBody = route.params.stream().anyMatch(p -> p.kind == MethodParam.Kind.BODY);
                if (hasBody) {
                    json.append("        \"requestBody\": {\n");
                    json.append("          \"required\": true,\n");
                    json.append("          \"content\": {\n");
                    json.append("            \"application/json\": {\n");
                    json.append("              \"schema\": { \"type\": \"string\" }\n");
                    json.append("            }\n");
                    json.append("          }\n");
                    json.append("        },\n");
                }

                // Responses
                json.append("        \"responses\": {\n");
                if (!route.responses.isEmpty()) {
                    for (int ri = 0; ri < route.responses.size(); ri++) {
                        ApiResponseInfo resp = route.responses.get(ri);
                        json.append("          ").append(jsonString(String.valueOf(resp.code))).append(": {\n");
                        json.append("            \"description\": ").append(jsonString(resp.description));
                        if (!resp.mediaType.isEmpty()) {
                            json.append(",\n            \"content\": {\n");
                            json.append("              ").append(jsonString(resp.mediaType)).append(": {\n");
                            json.append("                \"schema\": { \"type\": \"string\" }\n");
                            json.append("              }\n");
                            json.append("            }");
                        }
                        json.append("\n          }");
                        if (ri < route.responses.size() - 1) json.append(",");
                        json.append("\n");
                    }
                } else {
                    json.append("          \"200\": {\n");
                    json.append("            \"description\": \"Successful response\"\n");
                    json.append("          }\n");
                }
                json.append("        }\n");

                json.append("      }");
                if (r < routes.size() - 1) json.append(",");
                json.append("\n");
            }
            json.append("    }");
            if (pathIndex < pathMap.size() - 1) json.append(",");
            json.append("\n");
            pathIndex++;
        }
        json.append("  }\n");
        json.append("}\n");

        return json.toString();
    }

    private void writeOpenApiResource(String openApiJson) {
        try {
            FileObject resource = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", "openapi.json");
            try (PrintWriter out = new PrintWriter(resource.openWriter())) {
                out.print(openApiJson);
            }
        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    "Failed to generate openapi.json: " + e.getMessage());
        }
    }

    private String jsonString(String value) {
        if (value == null) return "\"\"";
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                + "\"";
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

    private record ResourceClass(String qualifiedName, String simpleName, List<RouteMethod> methods,
                                 String tagName, String tagDescription, ApiInfo apiInfo) {}

    private record RouteMethod(String httpMethod, String path, String methodName, List<MethodParam> params,
                               String summary, String description, List<ApiResponseInfo> responses) {}

    private record MethodParam(Kind kind, String name, String typeName) {
        enum Kind { PATH, QUERY, BODY, REQUEST }
    }

    private record ApiResponseInfo(int code, String description, String mediaType) {}

    private record RouteWithTag(RouteMethod route, String tagName) {}
}
