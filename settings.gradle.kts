pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")

            credentials.username = "mapbox"
            // Use the secret token stored in gradle.properties as the password
            credentials.password = "sk.eyJ1Ijoicml6OTkiLCJhIjoiY2xyaXFmYjNyMGFmazJrb3NtY3JocXVldCJ9.U9ul77d_G2ZFGcf6z04oIQ"
            authentication.create<BasicAuthentication>("basic")
        }
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "passengerApp"
include(":app")
 