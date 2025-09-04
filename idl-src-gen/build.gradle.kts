plugins {
  id("software.sava.build.feature.jlink")
}

jlinkApplication {
  applicationName = "idl-src-gen"
  mainClass = "software.sava.idl.generator.Entrypoint"
  mainModule = "software.sava.idl_src_gen"
  noHeaderFiles = true
  noManPages = true
  generateCdsArchive = true
}

testModuleInfo {
  requires("org.junit.jupiter.api")
  runtimeOnly("org.junit.jupiter.engine")
}
