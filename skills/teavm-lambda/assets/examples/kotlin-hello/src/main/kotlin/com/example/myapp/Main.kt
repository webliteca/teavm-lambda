package com.example.myapp

import ca.weblite.teavmlambda.dsl.*

fun main() = app {
    routes {
        "/hello" {
            get { ok { "message" to "Hello, World!" } }
            "/{name}" {
                get { ok { "message" to "Hello, ${path("name")}!" } }
            }
        }
    }
}
