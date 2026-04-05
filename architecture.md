```mermaid
block-beta
    columns 5

    %% ═══════════════════════════════════════════════════════════
    %% LAYER 1: USER APPLICATION (WORA)
    %% ═══════════════════════════════════════════════════════════

    block:UserApp:5
        columns 3
        space
        AppLabel["USER APPLICATION (WORA)"]
        space
        Demo["teavm-lambda-demo\nteavm-lambda-demo-cloudrun\n\n• Main.java\n• UsersResource.java\n• HealthResource.java"]
    end

    space:5

    %% ═══════════════════════════════════════════════════════════
    %% LAYER 2: PLATFORM-NEUTRAL API (Pure Java)
    %% ═══════════════════════════════════════════════════════════

    block:API:5
        columns 5
        space
        ApiLabel["PLATFORM-NEUTRAL API (Pure Java — no platform deps)"]
        space:3
        Core["teavm-lambda-core\n\n• Router\n• Request / Response\n• Platform → PlatformAdapter\n• Resources → ResourceLoader\n• @Path @GET @POST ..."]
        DbApi["teavm-lambda-db-api\n\n• Database → DatabaseProvider\n• DbResult / DbRow\n• Json → JsonProvider\n• DatabaseFactory"]
        LogApi["teavm-lambda-logging\n\n• Logger → LogHandler"]
        SentryApi["teavm-lambda-sentry\n\n• Sentry → SentryHandler"]
        Processor["teavm-lambda-processor\n\n• RouteProcessor\n  (generates GeneratedRouter\n   at compile time)"]
    end

    space:5

    %% ═══════════════════════════════════════════════════════════
    %% LAYER 3: PLATFORM IMPLEMENTATIONS
    %% ═══════════════════════════════════════════════════════════

    block:JsImpl:2
        columns 1
        JsLabel["NODE.JS / TeaVM"]
        CoreJs["teavm-lambda-core-js\n\n• NodeResourceLoader\n• NodeLogHandler\n• NodeSentryHandler"]
        AdapterLambda["teavm-lambda-adapter-lambda\n\n• LambdaPlatformAdapter\n• LambdaAdapter"]
        AdapterCloudrun["teavm-lambda-adapter-cloudrun\n\n• CloudRunPlatformAdapter\n• CloudRunAdapter"]
        DbJs["teavm-lambda-db\n\n• JsDatabaseProvider\n• JsDatabase / JsDbRow\n• JsJsonProvider"]
    end

    block:ParityCheck:1
        columns 1
        ParityLabel["PARITY\nENFORCEMENT"]
        SpiCheck["check-spi-parity.sh\n\nScans META-INF/services\nacross all modules.\nFails if any SPI lacks\nJS or JVM impl."]
        TestParity["run-parity-tests.sh\n\nBuilds with -P teavm\nthen -P jvm.\nRuns same test.sh\nagainst both."]
        IsAvail["isAvailable()\n\non Platform,\nDatabaseFactory,\nJson, Logger, Sentry"]
    end

    block:JvmImpl:2
        columns 1
        JvmLabel["JVM"]
        CoreJvm["teavm-lambda-core-jvm\n\n• JvmLogHandler\n• NoOpSentryHandler"]
        AdapterLambdaJvm["teavm-lambda-adapter-lambda-jvm\n\n• LambdaJvmPlatformAdapter\n• JvmLambdaAdapter"]
        AdapterHttp["teavm-lambda-adapter-httpserver\n\n• HttpServerPlatformAdapter\n• HttpServerAdapter"]
        DbJvm["teavm-lambda-db-jvm\n\n• JdbcDatabaseProvider\n• JdbcDatabase / JdbcDbRow\n• JvmJsonUtil"]
    end

    %% ═══════════════════════════════════════════════════════════
    %% RELATIONSHIPS
    %% ═══════════════════════════════════════════════════════════

    Demo --> Core
    Demo --> DbApi
    Core --> LogApi
    Core --> SentryApi

    CoreJs --> Core
    CoreJs --> LogApi
    CoreJs --> SentryApi
    AdapterLambda --> CoreJs
    AdapterCloudrun --> CoreJs
    DbJs --> DbApi

    CoreJvm --> LogApi
    CoreJvm --> SentryApi
    AdapterLambdaJvm --> Core
    AdapterHttp --> Core
    DbJvm --> DbApi

    Processor --> Core

    %% ═══════════════════════════════════════════════════════════
    %% STYLING
    %% ═══════════════════════════════════════════════════════════

    style UserApp fill:#e8f5e9,stroke:#2e7d32
    style Demo fill:#e8f5e9,stroke:#2e7d32
    style AppLabel fill:#e8f5e9,stroke:#e8f5e9,color:#2e7d32

    style API fill:#e3f2fd,stroke:#1565c0
    style Core fill:#e3f2fd,stroke:#1565c0
    style DbApi fill:#e3f2fd,stroke:#1565c0
    style LogApi fill:#e3f2fd,stroke:#1565c0
    style SentryApi fill:#e3f2fd,stroke:#1565c0
    style Processor fill:#fffde7,stroke:#f57f17
    style ApiLabel fill:#e3f2fd,stroke:#e3f2fd,color:#1565c0

    style JsImpl fill:#fce4ec,stroke:#c62828
    style CoreJs fill:#fce4ec,stroke:#c62828
    style AdapterLambda fill:#fce4ec,stroke:#c62828
    style AdapterCloudrun fill:#fce4ec,stroke:#c62828
    style DbJs fill:#fce4ec,stroke:#c62828
    style JsLabel fill:#fce4ec,stroke:#fce4ec,color:#c62828

    style JvmImpl fill:#f3e5f5,stroke:#6a1b9a
    style CoreJvm fill:#f3e5f5,stroke:#6a1b9a
    style AdapterLambdaJvm fill:#f3e5f5,stroke:#6a1b9a
    style AdapterHttp fill:#f3e5f5,stroke:#6a1b9a
    style DbJvm fill:#f3e5f5,stroke:#6a1b9a
    style JvmLabel fill:#f3e5f5,stroke:#f3e5f5,color:#6a1b9a

    style ParityCheck fill:#fff8e1,stroke:#ff8f00
    style SpiCheck fill:#fff8e1,stroke:#ff8f00
    style TestParity fill:#fff8e1,stroke:#ff8f00
    style IsAvail fill:#fff8e1,stroke:#ff8f00
    style ParityLabel fill:#fff8e1,stroke:#fff8e1,color:#ff8f00
```

## Module-to-Layer Mapping

| Layer | Maven Module | Contents | Platform |
|-------|-------------|----------|----------|
| **User App** | `teavm-lambda-demo` | Main, UsersResource, HealthResource | WORA |
| **User App** | `teavm-lambda-demo-cloudrun` | Main, UsersResource, HealthResource | WORA |
| | | | |
| **API** | `teavm-lambda-core` | Router, Request, Response, Platform, Resources, annotations | Pure Java |
| **API** | `teavm-lambda-db-api` | Database, DbResult, DbRow, Json, DatabaseFactory | Pure Java |
| **API** | `teavm-lambda-logging` | Logger, LogHandler | Pure Java |
| **API** | `teavm-lambda-sentry` | Sentry, SentryHandler | Pure Java |
| **API** | `teavm-lambda-processor` | RouteProcessor (compile-time codegen) | Pure Java |
| | | | |
| **JS Impl** | `teavm-lambda-core-js` | NodeResourceLoader, NodeLogHandler, NodeSentryHandler | Node.js |
| **JS Impl** | `teavm-lambda-adapter-lambda` | LambdaPlatformAdapter, LambdaAdapter | Node.js |
| **JS Impl** | `teavm-lambda-adapter-cloudrun` | CloudRunPlatformAdapter, CloudRunAdapter | Node.js |
| **JS Impl** | `teavm-lambda-db` | JsDatabaseProvider, JsDatabase, JsJsonProvider | Node.js |
| | | | |
| **JVM Impl** | `teavm-lambda-core-jvm` | JvmLogHandler, NoOpSentryHandler | JVM |
| **JVM Impl** | `teavm-lambda-adapter-lambda-jvm` | LambdaJvmPlatformAdapter, JvmLambdaAdapter | JVM |
| **JVM Impl** | `teavm-lambda-adapter-httpserver` | HttpServerPlatformAdapter, HttpServerAdapter | JVM |
| **JVM Impl** | `teavm-lambda-db-jvm` | JdbcDatabaseProvider, JdbcDatabase, JvmJsonUtil | JVM |
| | | | |
| **Parity** | `check-spi-parity.sh` | Validates every SPI has both JS + JVM in META-INF/services | Build-time |
| **Parity** | `run-parity-tests.sh` | Runs same tests against both `-P teavm` and `-P jvm` builds | Test-time |
| **Parity** | `isAvailable()` on every factory | Runtime feature detection | Runtime |

## SPI Symmetry

Each concern follows the same pattern — API module defines interface + factory, impl modules register via `META-INF/services`:

| Concern | API Module | Factory | SPI Interface | JS Module | JVM Module |
|---------|-----------|---------|---------------|-----------|------------|
| Server | core | `Platform` | `PlatformAdapter` | adapter-lambda / adapter-cloudrun | adapter-lambda-jvm / adapter-httpserver |
| Database | db-api | `DatabaseFactory` | `DatabaseProvider` | db | db-jvm |
| JSON | db-api | `Json` | `JsonProvider` | db | db-jvm |
| Resources | core | `Resources` | `ResourceLoader` | core-js | (built-in fallback) |
| Logging | logging | `Logger` | `LogHandler` | core-js | core-jvm |
| Error tracking | sentry | `Sentry` | `SentryHandler` | core-js | core-jvm |
