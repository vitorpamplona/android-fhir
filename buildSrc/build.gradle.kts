plugins {
  `kotlin-dsl`
}

repositories {
  google()
  gradlePluginPortal()
  mavenCentral()
}

configurations {
  all {
    resolutionStrategy {
      force("com.fasterxml.jackson.core:jackson-annotations:2.15.2")
      force("com.fasterxml.jackson.core:jackson-core:2.15.2")
      force("com.fasterxml.jackson.core:jackson-databind:2.15.2")
      force("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.15.2")
      force("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:2.15.2")
      force("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")
      force("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    }
  }
}

dependencies {
  implementation("com.diffplug.spotless:spotless-plugin-gradle:6.21.0")

  implementation("com.android.tools.build:gradle:8.1.2")

  implementation("app.cash.licensee:licensee-gradle-plugin:1.3.0")
  implementation("com.osacky.flank.gradle:fladle:0.17.4")

  implementation("com.spotify.ruler:ruler-gradle-plugin:1.2.1")

  implementation("ca.uhn.hapi.fhir:hapi-fhir-structures-r4:6.8.0")
  implementation("com.squareup:kotlinpoet:1.12.0")
}
