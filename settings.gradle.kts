pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "My Application"

// Write .env file at initialization phase to prevent secrets race conditions and caching issues
val envFile = java.io.File(settingsDir, ".env")
val geminiKey = System.getenv("GEMINI_API_KEY") ?: ""
val footballKey = System.getenv("FOOTBALL_API_KEY") ?: ""

val envLines = mutableListOf<String>()
if (geminiKey.isNotEmpty()) {
    envLines.add("GEMINI_API_KEY=$geminiKey")
} else {
    envLines.add("GEMINI_API_KEY=MY_GEMINI_API_KEY")
}

if (footballKey.isNotEmpty()) {
    envLines.add("FOOTBALL_API_KEY=$footballKey")
} else {
    envLines.add("FOOTBALL_API_KEY=MY_FOOTBALL_API_KEY")
}

envFile.writeText(envLines.joinToString("\n") + "\n")

include(":app")
