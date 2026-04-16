# POM Templates

> Copy-pasteable pom.xml files for each deployment target. Replace `com.example` with your groupId and `my-app` with your artifactId.

**Prerequisites**: teavm-lambda artifacts must be available in your local Maven repo or Maven Central.

---

## 1. JVM Standalone Server (jvm-server)

Simplest setup. Good for local development and Docker deployment.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <teavm-lambda.version>0.1.6</teavm-lambda.version>
    </properties>

    <dependencies>
        <!-- Core API (annotations, Request, Response, Router, Container, etc.) -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-core</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <!-- JVM platform implementations -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-core-jvm</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <!-- JDK HttpServer adapter (zero external deps) -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-adapter-httpserver</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <!-- Annotation processor (compile-time only) -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-processor</artifactId>
            <version>${teavm-lambda.version}</version>
            <scope>provided</scope>
        </dependency>

        <!-- Uncomment to add PostgreSQL support -->
        <!--
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-db-api</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-db-jvm</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>
        -->

        <!-- Uncomment to add JWT auth -->
        <!--
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-auth-jvm</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        -->

        <!-- Uncomment to add response compression -->
        <!--
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-compression-jvm</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        -->
    </dependencies>

    <build>
        <plugins>
            <!-- Annotation processor for GeneratedRouter and GeneratedContainer -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-processor</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </path>
                        <path>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-core</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <!-- Uber JAR with ServiceLoader support -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.example.myapp.Main</mainClass>
                                </transformer>
                                <!-- REQUIRED: merges META-INF/services for SPI discovery -->
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Build: `mvn clean package`
Run: `java -jar target/my-app-1.0.0.jar`

---

## 2. AWS Lambda (teavm / Node.js)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <teavm-lambda.version>0.1.6</teavm-lambda.version>
        <teavm.version>0.13.1</teavm.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-core</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-adapter-lambda</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-processor</artifactId>
            <version>${teavm-lambda.version}</version>
            <scope>provided</scope>
        </dependency>
        <!-- TeaVM runtime -->
        <dependency>
            <groupId>org.teavm</groupId>
            <artifactId>teavm-classlib</artifactId>
            <version>${teavm.version}</version>
        </dependency>
        <dependency>
            <groupId>org.teavm</groupId>
            <artifactId>teavm-jso</artifactId>
            <version>${teavm.version}</version>
        </dependency>
        <dependency>
            <groupId>org.teavm</groupId>
            <artifactId>teavm-jso-apis</artifactId>
            <version>${teavm.version}</version>
        </dependency>

        <!-- Uncomment for PostgreSQL -->
        <!--
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-db-api</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-db</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        -->
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-processor</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </path>
                        <path>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-core</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <!-- TeaVM: compile Java to JavaScript -->
            <plugin>
                <groupId>org.teavm</groupId>
                <artifactId>teavm-maven-plugin</artifactId>
                <version>${teavm.version}</version>
                <executions>
                    <execution>
                        <goals><goal>compile</goal></goals>
                        <configuration>
                            <mainClass>com.example.myapp.Main</mainClass>
                            <targetType>JAVASCRIPT</targetType>
                            <targetDirectory>${project.build.directory}/lambda</targetDirectory>
                            <targetFileName>teavm-app.js</targetFileName>
                            <minifying>false</minifying>
                            <optimizationLevel>SIMPLE</optimizationLevel>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- Copy bootstrap + package.json to lambda output -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-resources-plugin</artifactId>
                <version>3.3.1</version>
                <executions>
                    <execution>
                        <id>copy-lambda-files</id>
                        <phase>package</phase>
                        <goals><goal>copy-resources</goal></goals>
                        <configuration>
                            <outputDirectory>${project.build.directory}/lambda</outputDirectory>
                            <resources>
                                <resource>
                                    <directory>${project.basedir}/docker</directory>
                                    <includes>
                                        <include>package.json</include>
                                        <include>bootstrap.js</include>
                                    </includes>
                                </resource>
                            </resources>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            <!-- Rename bootstrap.js to index.js and run npm install -->
            <plugin>
                <groupId>org.codehaus.mojo</groupId>
                <artifactId>exec-maven-plugin</artifactId>
                <version>3.1.0</version>
                <executions>
                    <execution>
                        <id>rename-bootstrap</id>
                        <phase>package</phase>
                        <goals><goal>exec</goal></goals>
                        <configuration>
                            <executable>mv</executable>
                            <workingDirectory>${project.build.directory}/lambda</workingDirectory>
                            <arguments>
                                <argument>bootstrap.js</argument>
                                <argument>index.js</argument>
                            </arguments>
                        </configuration>
                    </execution>
                    <execution>
                        <id>npm-install</id>
                        <phase>package</phase>
                        <goals><goal>exec</goal></goals>
                        <configuration>
                            <executable>npm</executable>
                            <workingDirectory>${project.build.directory}/lambda</workingDirectory>
                            <arguments>
                                <argument>install</argument>
                                <argument>--production</argument>
                            </arguments>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Build: `mvn clean package`
Output: `target/lambda/` directory, deploy via SAM.

---

## 3. Google Cloud Run (teavm / Node.js)

Same as the Lambda template but replace:
- `teavm-lambda-adapter-lambda` → `teavm-lambda-adapter-cloudrun`
- `targetDirectory` → `${project.build.directory}/cloudrun`
- Copy `server.js` instead of `bootstrap.js`
- Replace the npm/rename executions with a single copy + npm install targeting `cloudrun/`

See the `cloudrun-deploy` example project for the complete working pom.xml.

---

## 4. WAR (Servlet Container)

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-app</artifactId>
    <version>1.0.0</version>
    <packaging>war</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <teavm-lambda.version>0.1.6</teavm-lambda.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-core</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-core-jvm</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <!-- WAR adapter (provided — bundled in the WAR) -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-adapter-war</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <!-- Servlet API (provided by the container) -->
        <dependency>
            <groupId>jakarta.servlet</groupId>
            <artifactId>jakarta.servlet-api</artifactId>
            <version>6.0.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-processor</artifactId>
            <version>${teavm-lambda.version}</version>
            <scope>provided</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.13.0</version>
                <configuration>
                    <source>21</source>
                    <target>21</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-processor</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </path>
                        <path>
                            <groupId>ca.weblite</groupId>
                            <artifactId>teavm-lambda-core</artifactId>
                            <version>${teavm-lambda.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-war-plugin</artifactId>
                <version>3.4.0</version>
            </plugin>
        </plugins>
    </build>
</project>
```

Build: `mvn clean package`
Deploy: `target/my-app-1.0.0.war` to Tomcat 10.1+, TomEE 10+, or Jetty 12+.

---

## 5. Kotlin JVM Standalone Server (with DSL)

Uses the Kotlin DSL module for routing, middleware, and DI.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>my-kotlin-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <teavm-lambda.version>0.1.6</teavm-lambda.version>
        <kotlin.version>1.9.25</kotlin.version>
    </properties>

    <dependencies>
        <!-- Kotlin DSL (brings in teavm-lambda-core and teavm-lambda-db-api) -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-kotlin</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <!-- JVM platform implementations -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-core-jvm</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <!-- JDK HttpServer adapter -->
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-adapter-httpserver</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <!-- Kotlin stdlib -->
        <dependency>
            <groupId>org.jetbrains.kotlin</groupId>
            <artifactId>kotlin-stdlib</artifactId>
            <version>${kotlin.version}</version>
        </dependency>

        <!-- Uncomment to add PostgreSQL support -->
        <!--
        <dependency>
            <groupId>ca.weblite</groupId>
            <artifactId>teavm-lambda-db-jvm</artifactId>
            <version>${teavm-lambda.version}</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.3</version>
        </dependency>
        -->
    </dependencies>

    <build>
        <sourceDirectory>${project.basedir}/src/main/kotlin</sourceDirectory>
        <plugins>
            <!-- Kotlin compiler -->
            <plugin>
                <groupId>org.jetbrains.kotlin</groupId>
                <artifactId>kotlin-maven-plugin</artifactId>
                <version>${kotlin.version}</version>
                <executions>
                    <execution>
                        <id>compile</id>
                        <goals><goal>compile</goal></goals>
                    </execution>
                </executions>
            </plugin>
            <!-- Uber JAR with ServiceLoader support -->
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals><goal>shade</goal></goals>
                        <configuration>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.example.myapp.MainKt</mainClass>
                                </transformer>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

Build: `mvn clean package`
Run: `java -jar target/my-kotlin-app-1.0.0.jar`

**Note:** The Kotlin DSL does not use the annotation processor — routes are defined in code via `routes { }`. No `GeneratedRouter`/`GeneratedContainer` are generated. You can still use annotation-based routing in Kotlin by adding `teavm-lambda-processor` as in the Java templates.
