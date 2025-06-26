plugins {
  id("software.sava.build.feature.publish-maven-central")
}

dependencies {
  nmcpAggregation(project(":anchor-src-gen"))
}

tasks.register("publishToGitHubPackages") {
  group = "publishing"
  dependsOn(
    ":anchor-src-gen:publishMavenJavaPublicationToSavaGithubPackagesRepository"
  )
}
