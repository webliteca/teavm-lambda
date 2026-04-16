/**
 * Project generator for teavm-lambda starter projects.
 * Called from ProjectInitializer.java via @JSBody.
 */
window.generateTeavmLambdaProject = function(groupId, artifactId, packageName, language) {
    var zip = new JSZip();
    var pkgPath = packageName.replace(/\./g, '/');
    var isKotlin = (language === 'kotlin');
    var v = '0.1.6';
    var tv = '0.13.1';
    var root = artifactId + '/';

    // ========== pom.xml ==========
    var pom = isKotlin
        ? buildKotlinPom(groupId, artifactId, packageName, v, tv)
        : buildJavaPom(groupId, artifactId, packageName, v, tv);
    zip.file(root + 'pom.xml', pom);

    // ========== Source files ==========
    if (isKotlin) {
        zip.file(root + 'src/main/kotlin/' + pkgPath + '/Main.kt', buildKotlinMain(packageName));
    } else {
        zip.file(root + 'src/main/java/' + pkgPath + '/HelloResource.java', buildJavaResource(packageName));
        zip.file(root + 'src/main/java/' + pkgPath + '/Main.java', buildJavaMain(packageName));
    }

    // ========== Lambda bootstrap files ==========
    zip.file(root + 'docker/bootstrap.js',
        "const teavm = require('./teavm-app.js');\n" +
        "teavm.main([]);\n" +
        "exports.handler = async (event, context) => {\n" +
        "    return global.__teavmLambdaHandler(event, context);\n" +
        "};\n");

    zip.file(root + 'docker/server.js',
        "const teavm = require('./teavm-app.js');\n" +
        "teavm.main([]);\n");

    zip.file(root + 'docker/package.json',
        '{\n  "name": "' + artifactId + '",\n  "version": "1.0.0",\n  "dependencies": {\n    "pg": "^8.13.0"\n  }\n}\n');

    // ========== template.yaml (SAM) ==========
    zip.file(root + 'template.yaml', buildSamTemplate());

    // ========== Dockerfile ==========
    zip.file(root + 'Dockerfile', buildDockerfile(artifactId));

    // ========== run.sh ==========
    zip.file(root + 'run.sh', buildRunScript(artifactId), {unixPermissions: '755'});

    // ========== README ==========
    zip.file(root + 'README.md', buildReadme(artifactId));

    // ========== Download ==========
    zip.generateAsync({type: 'blob', platform: 'UNIX'}).then(function(blob) {
        var link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = artifactId + '.zip';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        URL.revokeObjectURL(link.href);
    });
};

// ========== Java pom.xml with jvm-server, lambda, cloudrun profiles ==========
function buildJavaPom(groupId, artifactId, packageName, v, tv) {
    var mainClass = packageName + '.Main';
    return '<?xml version="1.0" encoding="UTF-8"?>\n' +
'<project xmlns="http://maven.apache.org/POM/4.0.0"\n' +
'         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n' +
'         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0\n' +
'           http://maven.apache.org/xsd/maven-4.0.0.xsd">\n' +
'    <modelVersion>4.0.0</modelVersion>\n\n' +
'    <groupId>' + groupId + '</groupId>\n' +
'    <artifactId>' + artifactId + '</artifactId>\n' +
'    <version>1.0.0-SNAPSHOT</version>\n\n' +
'    <properties>\n' +
'        <maven.compiler.source>21</maven.compiler.source>\n' +
'        <maven.compiler.target>21</maven.compiler.target>\n' +
'        <teavm.version>' + tv + '</teavm.version>\n' +
'        <teavm-lambda.version>' + v + '</teavm-lambda.version>\n' +
'    </properties>\n\n' +
'    <!-- Platform-neutral dependency -->\n' +
'    <dependencies>\n' +
'        <dependency>\n' +
'            <groupId>ca.weblite</groupId>\n' +
'            <artifactId>teavm-lambda-core</artifactId>\n' +
'            <version>${teavm-lambda.version}</version>\n' +
'        </dependency>\n' +
'    </dependencies>\n\n' +
'    <profiles>\n' +
        jvmServerProfile(mainClass, v) +
        lambdaProfile(mainClass, v, tv) +
        cloudrunProfile(mainClass, v, tv) +
'    </profiles>\n' +
'</project>\n';
}

// ========== Kotlin pom.xml with jvm-server, lambda, cloudrun profiles ==========
function buildKotlinPom(groupId, artifactId, packageName, v, tv) {
    var mainClass = packageName + '.MainKt';
    return '<?xml version="1.0" encoding="UTF-8"?>\n' +
'<project xmlns="http://maven.apache.org/POM/4.0.0"\n' +
'         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"\n' +
'         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0\n' +
'           http://maven.apache.org/xsd/maven-4.0.0.xsd">\n' +
'    <modelVersion>4.0.0</modelVersion>\n\n' +
'    <groupId>' + groupId + '</groupId>\n' +
'    <artifactId>' + artifactId + '</artifactId>\n' +
'    <version>1.0.0-SNAPSHOT</version>\n\n' +
'    <properties>\n' +
'        <kotlin.version>1.9.25</kotlin.version>\n' +
'        <teavm.version>' + tv + '</teavm.version>\n' +
'        <teavm-lambda.version>' + v + '</teavm-lambda.version>\n' +
'    </properties>\n\n' +
'    <dependencies>\n' +
'        <dependency>\n' +
'            <groupId>ca.weblite</groupId>\n' +
'            <artifactId>teavm-lambda-kotlin</artifactId>\n' +
'            <version>${teavm-lambda.version}</version>\n' +
'        </dependency>\n' +
'        <dependency>\n' +
'            <groupId>org.jetbrains.kotlin</groupId>\n' +
'            <artifactId>kotlin-stdlib</artifactId>\n' +
'            <version>${kotlin.version}</version>\n' +
'        </dependency>\n' +
'    </dependencies>\n\n' +
'    <profiles>\n' +
        kotlinJvmServerProfile(mainClass, v) +
        kotlinLambdaProfile(mainClass, v, tv) +
        kotlinCloudrunProfile(mainClass, v, tv) +
'    </profiles>\n' +
'</project>\n';
}

// ========== Shared profile building blocks ==========

function compilerPlugin(v) {
    return '                    <plugin>\n' +
'                        <groupId>org.apache.maven.plugins</groupId>\n' +
'                        <artifactId>maven-compiler-plugin</artifactId>\n' +
'                        <version>3.13.0</version>\n' +
'                        <configuration>\n' +
'                            <annotationProcessorPaths>\n' +
'                                <path>\n' +
'                                    <groupId>ca.weblite</groupId>\n' +
'                                    <artifactId>teavm-lambda-processor</artifactId>\n' +
'                                    <version>${teavm-lambda.version}</version>\n' +
'                                </path>\n' +
'                                <path>\n' +
'                                    <groupId>ca.weblite</groupId>\n' +
'                                    <artifactId>teavm-lambda-core</artifactId>\n' +
'                                    <version>${teavm-lambda.version}</version>\n' +
'                                </path>\n' +
'                            </annotationProcessorPaths>\n' +
'                        </configuration>\n' +
'                    </plugin>\n';
}

function shadePlugin(mainClass) {
    return '                    <plugin>\n' +
'                        <groupId>org.apache.maven.plugins</groupId>\n' +
'                        <artifactId>maven-shade-plugin</artifactId>\n' +
'                        <version>3.5.3</version>\n' +
'                        <executions>\n' +
'                            <execution>\n' +
'                                <phase>package</phase>\n' +
'                                <goals><goal>shade</goal></goals>\n' +
'                                <configuration>\n' +
'                                    <transformers>\n' +
'                                        <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">\n' +
'                                            <mainClass>' + mainClass + '</mainClass>\n' +
'                                        </transformer>\n' +
'                                        <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>\n' +
'                                    </transformers>\n' +
'                                </configuration>\n' +
'                            </execution>\n' +
'                        </executions>\n' +
'                    </plugin>\n';
}

function teavmPlugin(mainClass, tv, targetDir, targetFileName) {
    return '                    <plugin>\n' +
'                        <groupId>org.teavm</groupId>\n' +
'                        <artifactId>teavm-maven-plugin</artifactId>\n' +
'                        <version>${teavm.version}</version>\n' +
'                        <executions>\n' +
'                            <execution>\n' +
'                                <goals><goal>compile</goal></goals>\n' +
'                                <configuration>\n' +
'                                    <mainClass>' + mainClass + '</mainClass>\n' +
'                                    <targetType>JAVASCRIPT</targetType>\n' +
'                                    <targetDirectory>${project.build.directory}/' + targetDir + '</targetDirectory>\n' +
'                                    <targetFileName>' + targetFileName + '</targetFileName>\n' +
'                                    <minifying>false</minifying>\n' +
'                                    <optimizationLevel>SIMPLE</optimizationLevel>\n' +
'                                </configuration>\n' +
'                            </execution>\n' +
'                        </executions>\n' +
'                    </plugin>\n';
}

function resourcesCopyPlugin(targetDir, includes) {
    return '                    <plugin>\n' +
'                        <groupId>org.apache.maven.plugins</groupId>\n' +
'                        <artifactId>maven-resources-plugin</artifactId>\n' +
'                        <version>3.3.1</version>\n' +
'                        <executions>\n' +
'                            <execution>\n' +
'                                <id>copy-runtime-files</id>\n' +
'                                <phase>package</phase>\n' +
'                                <goals><goal>copy-resources</goal></goals>\n' +
'                                <configuration>\n' +
'                                    <outputDirectory>${project.build.directory}/' + targetDir + '</outputDirectory>\n' +
'                                    <resources>\n' +
'                                        <resource>\n' +
'                                            <directory>${project.basedir}/docker</directory>\n' +
'                                            <includes>\n' +
                                                includes.map(function(f) { return '                                                <include>' + f + '</include>\n'; }).join('') +
'                                            </includes>\n' +
'                                        </resource>\n' +
'                                    </resources>\n' +
'                                </configuration>\n' +
'                            </execution>\n' +
'                        </executions>\n' +
'                    </plugin>\n';
}

function execPlugins(targetDir, renameFrom, renameTo) {
    var execs = '';
    if (renameFrom) {
        execs += '                            <execution>\n' +
'                                <id>rename-bootstrap</id>\n' +
'                                <phase>package</phase>\n' +
'                                <goals><goal>exec</goal></goals>\n' +
'                                <configuration>\n' +
'                                    <executable>mv</executable>\n' +
'                                    <workingDirectory>${project.build.directory}/' + targetDir + '</workingDirectory>\n' +
'                                    <arguments>\n' +
'                                        <argument>' + renameFrom + '</argument>\n' +
'                                        <argument>' + renameTo + '</argument>\n' +
'                                    </arguments>\n' +
'                                </configuration>\n' +
'                            </execution>\n';
    }
    execs += '                            <execution>\n' +
'                                <id>npm-install</id>\n' +
'                                <phase>package</phase>\n' +
'                                <goals><goal>exec</goal></goals>\n' +
'                                <configuration>\n' +
'                                    <executable>npm</executable>\n' +
'                                    <workingDirectory>${project.build.directory}/' + targetDir + '</workingDirectory>\n' +
'                                    <arguments>\n' +
'                                        <argument>install</argument>\n' +
'                                        <argument>--production</argument>\n' +
'                                    </arguments>\n' +
'                                </configuration>\n' +
'                            </execution>\n';

    return '                    <plugin>\n' +
'                        <groupId>org.codehaus.mojo</groupId>\n' +
'                        <artifactId>exec-maven-plugin</artifactId>\n' +
'                        <version>3.1.0</version>\n' +
'                        <executions>\n' + execs +
'                        </executions>\n' +
'                    </plugin>\n';
}

function teavmDeps(adapterArtifact, v) {
    return '                <dependency>\n' +
'                    <groupId>ca.weblite</groupId>\n' +
'                    <artifactId>' + adapterArtifact + '</artifactId>\n' +
'                    <version>${teavm-lambda.version}</version>\n' +
'                </dependency>\n' +
'                <dependency>\n' +
'                    <groupId>org.teavm</groupId>\n' +
'                    <artifactId>teavm-classlib</artifactId>\n' +
'                    <version>${teavm.version}</version>\n' +
'                </dependency>\n' +
'                <dependency>\n' +
'                    <groupId>org.teavm</groupId>\n' +
'                    <artifactId>teavm-jso</artifactId>\n' +
'                    <version>${teavm.version}</version>\n' +
'                </dependency>\n' +
'                <dependency>\n' +
'                    <groupId>org.teavm</groupId>\n' +
'                    <artifactId>teavm-jso-apis</artifactId>\n' +
'                    <version>${teavm.version}</version>\n' +
'                </dependency>\n';
}

function jvmDeps(adapterArtifact) {
    return '                <dependency>\n' +
'                    <groupId>ca.weblite</groupId>\n' +
'                    <artifactId>teavm-lambda-core-jvm</artifactId>\n' +
'                    <version>${teavm-lambda.version}</version>\n' +
'                </dependency>\n' +
'                <dependency>\n' +
'                    <groupId>ca.weblite</groupId>\n' +
'                    <artifactId>' + adapterArtifact + '</artifactId>\n' +
'                    <version>${teavm-lambda.version}</version>\n' +
'                </dependency>\n';
}

function kotlinPlugin() {
    return '                    <plugin>\n' +
'                        <groupId>org.jetbrains.kotlin</groupId>\n' +
'                        <artifactId>kotlin-maven-plugin</artifactId>\n' +
'                        <version>${kotlin.version}</version>\n' +
'                        <executions>\n' +
'                            <execution>\n' +
'                                <id>compile</id>\n' +
'                                <goals><goal>compile</goal></goals>\n' +
'                                <configuration><jvmTarget>21</jvmTarget></configuration>\n' +
'                            </execution>\n' +
'                        </executions>\n' +
'                    </plugin>\n';
}

// ========== Java profiles ==========

function jvmServerProfile(mainClass, v) {
    return '        <!-- JVM standalone server (default) -->\n' +
'        <profile>\n' +
'            <id>jvm-server</id>\n' +
'            <activation><activeByDefault>true</activeByDefault></activation>\n' +
'            <dependencies>\n' + jvmDeps('teavm-lambda-adapter-httpserver') + '            </dependencies>\n' +
'            <build>\n' +
'                <plugins>\n' + compilerPlugin(v) + shadePlugin(mainClass) + '                </plugins>\n' +
'            </build>\n' +
'        </profile>\n\n';
}

function lambdaProfile(mainClass, v, tv) {
    return '        <!-- AWS Lambda (TeaVM / Node.js) -->\n' +
'        <profile>\n' +
'            <id>lambda</id>\n' +
'            <dependencies>\n' + teavmDeps('teavm-lambda-adapter-lambda', v) + '            </dependencies>\n' +
'            <build>\n' +
'                <plugins>\n' +
                    compilerPlugin(v) +
                    teavmPlugin(mainClass, tv, 'lambda', 'teavm-app.js') +
                    resourcesCopyPlugin('lambda', ['bootstrap.js', 'package.json']) +
                    execPlugins('lambda', 'bootstrap.js', 'index.js') +
'                </plugins>\n' +
'            </build>\n' +
'        </profile>\n\n';
}

function cloudrunProfile(mainClass, v, tv) {
    return '        <!-- Google Cloud Run (TeaVM / Node.js) -->\n' +
'        <profile>\n' +
'            <id>cloudrun</id>\n' +
'            <dependencies>\n' + teavmDeps('teavm-lambda-adapter-cloudrun', v) + '            </dependencies>\n' +
'            <build>\n' +
'                <plugins>\n' +
                    compilerPlugin(v) +
                    teavmPlugin(mainClass, tv, 'cloudrun', 'teavm-app.js') +
                    resourcesCopyPlugin('cloudrun', ['server.js', 'package.json']) +
                    execPlugins('cloudrun', null, null) +
'                </plugins>\n' +
'            </build>\n' +
'        </profile>\n';
}

// ========== Kotlin profiles ==========

function kotlinJvmServerProfile(mainClass, v) {
    return '        <!-- JVM standalone server (default) -->\n' +
'        <profile>\n' +
'            <id>jvm-server</id>\n' +
'            <activation><activeByDefault>true</activeByDefault></activation>\n' +
'            <dependencies>\n' + jvmDeps('teavm-lambda-adapter-httpserver') + '            </dependencies>\n' +
'            <build>\n' +
'                <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>\n' +
'                <plugins>\n' + kotlinPlugin() + shadePlugin(mainClass) + '                </plugins>\n' +
'            </build>\n' +
'        </profile>\n\n';
}

function kotlinLambdaProfile(mainClass, v, tv) {
    return '        <!-- AWS Lambda (TeaVM / Node.js) -->\n' +
'        <profile>\n' +
'            <id>lambda</id>\n' +
'            <dependencies>\n' + teavmDeps('teavm-lambda-adapter-lambda', v) + '            </dependencies>\n' +
'            <build>\n' +
'                <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>\n' +
'                <plugins>\n' +
                    kotlinPlugin() +
                    teavmPlugin(mainClass, tv, 'lambda', 'teavm-app.js') +
                    resourcesCopyPlugin('lambda', ['bootstrap.js', 'package.json']) +
                    execPlugins('lambda', 'bootstrap.js', 'index.js') +
'                </plugins>\n' +
'            </build>\n' +
'        </profile>\n\n';
}

function kotlinCloudrunProfile(mainClass, v, tv) {
    return '        <!-- Google Cloud Run (TeaVM / Node.js) -->\n' +
'        <profile>\n' +
'            <id>cloudrun</id>\n' +
'            <dependencies>\n' + teavmDeps('teavm-lambda-adapter-cloudrun', v) + '            </dependencies>\n' +
'            <build>\n' +
'                <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>\n' +
'                <plugins>\n' +
                    kotlinPlugin() +
                    teavmPlugin(mainClass, tv, 'cloudrun', 'teavm-app.js') +
                    resourcesCopyPlugin('cloudrun', ['server.js', 'package.json']) +
                    execPlugins('cloudrun', null, null) +
'                </plugins>\n' +
'            </build>\n' +
'        </profile>\n';
}

// ========== Source files ==========

function buildJavaResource(packageName) {
    return 'package ' + packageName + ';\n\n' +
'import ca.weblite.teavmlambda.api.Response;\n' +
'import ca.weblite.teavmlambda.api.annotation.*;\n\n' +
'@Path("/hello")\n' +
'@Component\n' +
'@Singleton\n' +
'public class HelloResource {\n\n' +
'    @GET\n' +
'    public Response hello() {\n' +
'        return Response.ok("{\\"message\\":\\"Hello, World!\\"}")\n' +
'                .header("Content-Type", "application/json");\n' +
'    }\n\n' +
'    @GET\n' +
'    @Path("/{name}")\n' +
'    public Response helloName(@PathParam("name") String name) {\n' +
'        return Response.ok("{\\"message\\":\\"Hello, " + name + "!\\"}")\n' +
'                .header("Content-Type", "application/json");\n' +
'    }\n' +
'}\n';
}

function buildJavaMain(packageName) {
    return 'package ' + packageName + ';\n\n' +
'import ca.weblite.teavmlambda.api.Platform;\n' +
'import ca.weblite.teavmlambda.generated.GeneratedContainer;\n' +
'import ca.weblite.teavmlambda.generated.GeneratedRouter;\n\n' +
'public class Main {\n\n' +
'    public static void main(String[] args) throws Exception {\n' +
'        var container = new GeneratedContainer();\n' +
'        var router = new GeneratedRouter(container);\n' +
'        Platform.start(router);\n' +
'    }\n' +
'}\n';
}

function buildKotlinMain(packageName) {
    return 'package ' + packageName + '\n\n' +
'import ca.weblite.teavmlambda.dsl.*\n\n' +
'fun main() = app {\n' +
'    routes {\n' +
'        "/hello" {\n' +
'            get { ok { "message" to "Hello, World!" } }\n' +
'            "/{name}" {\n' +
'                get { ok { "message" to "Hello, ${path("name")}!" } }\n' +
'            }\n' +
'        }\n' +
'    }\n' +
'}\n';
}

// ========== Config files ==========

function buildSamTemplate() {
    return "AWSTemplateFormatVersion: '2010-09-09'\n" +
"Transform: AWS::Serverless-2016-10-31\n\n" +
"Globals:\n" +
"  Function:\n" +
"    Timeout: 30\n" +
"    MemorySize: 256\n" +
"    Runtime: nodejs22.x\n\n" +
"Resources:\n" +
"  ApiFunction:\n" +
"    Type: AWS::Serverless::Function\n" +
"    Properties:\n" +
"      CodeUri: target/lambda/\n" +
"      Handler: index.handler\n" +
"      Events:\n" +
"        RootApi:\n" +
"          Type: Api\n" +
"          Properties:\n" +
"            Path: /\n" +
"            Method: ANY\n" +
"        ProxyApi:\n" +
"          Type: Api\n" +
"          Properties:\n" +
"            Path: /{proxy+}\n" +
"            Method: ANY\n";
}

function buildDockerfile(artifactId) {
    return "FROM maven:3.9-eclipse-temurin-21 AS build\n" +
"WORKDIR /app\n" +
"COPY pom.xml .\n" +
"COPY src src\n" +
"COPY docker docker\n" +
"RUN mvn clean package -P cloudrun -q\n\n" +
"FROM node:22-slim\n" +
"WORKDIR /app\n" +
"COPY --from=build /app/target/cloudrun/ .\n" +
"RUN npm install --production\n" +
"EXPOSE 8080\n" +
'CMD ["node", "server.js"]\n';
}

function buildRunScript(artifactId) {
    return '#!/bin/bash\n' +
'set -e\n' +
'PROFILE="${1:-jvm-server}"\n' +
'PORT="${2:-8080}"\n\n' +
'echo "=== Building with profile: $PROFILE ==="\n' +
'mvn clean package -P "$PROFILE" -q\n\n' +
'case "$PROFILE" in\n' +
'    jvm-server)\n' +
'        echo "=== Starting JVM server on http://localhost:$PORT ==="\n' +
'        PORT=$PORT java -jar target/' + artifactId + '-1.0.0-SNAPSHOT.jar\n' +
'        ;;\n' +
'    lambda)\n' +
'        echo "=== Starting Lambda via SAM on http://localhost:3000 ==="\n' +
'        sam local start-api\n' +
'        ;;\n' +
'    cloudrun)\n' +
'        echo "=== Starting Node.js server on http://localhost:$PORT ==="\n' +
'        PORT=$PORT node target/cloudrun/server.js\n' +
'        ;;\n' +
'    *)\n' +
'        echo "Unknown profile: $PROFILE"\n' +
'        echo "Usage: ./run.sh [jvm-server|lambda|cloudrun] [port]"\n' +
'        exit 1\n' +
'        ;;\n' +
'esac\n';
}

function buildReadme(artifactId) {
    return "# " + artifactId + "\n\n" +
"A teavm-lambda project with three deployment targets.\n\n" +
"## Quick Start\n\n" +
"```bash\n" +
"./run.sh                  # JVM standalone (default, port 8080)\n" +
"./run.sh cloudrun         # TeaVM/Node.js (no Docker needed)\n" +
"./run.sh lambda           # TeaVM/Node.js via SAM (needs Docker)\n" +
"./run.sh jvm-server 3000  # JVM on custom port\n" +
"```\n\n" +
"## Build and Run (Manual)\n\n" +
"### JVM Standalone Server (default)\n\n" +
"```bash\n" +
"mvn clean package\n" +
"java -jar target/" + artifactId + "-1.0.0-SNAPSHOT.jar\n" +
"```\n\n" +
"### TeaVM / Node.js (no Docker)\n\n" +
"```bash\n" +
"mvn clean package -P cloudrun\n" +
"node target/cloudrun/server.js\n" +
"```\n\n" +
"### AWS Lambda (SAM)\n\n" +
"```bash\n" +
"mvn clean package -P lambda\n" +
"sam local start-api\n" +
"```\n\n" +
"### Docker (Cloud Run)\n\n" +
"```bash\n" +
"docker build -t " + artifactId + " .\n" +
"docker run -p 8080:8080 " + artifactId + "\n" +
"```\n\n" +
"## Test\n\n" +
"```bash\n" +
"curl http://localhost:8080/hello\n" +
'# {"message":"Hello, World!"}\n\n' +
"curl http://localhost:8080/hello/Alice\n" +
'# {"message":"Hello, Alice!"}\n' +
"```\n";
}
