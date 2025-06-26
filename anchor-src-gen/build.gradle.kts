plugins {
  id("software.sava.build.feature.jlink")
}

jlinkApplication {
  applicationName = "anchor-src-gen"
  mainClass = "software.sava.anchor.Entrypoint"
  mainModule = "software.sava.anchor_src_gen"
  noHeaderFiles = true
  noManPages = true
  generateCdsArchive = true
}

testModuleInfo {
  requires("org.junit.jupiter.api")
  runtimeOnly("org.junit.jupiter.engine")
}
