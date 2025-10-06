package software.sava.idl.generator.codama;

import software.sava.idl.generator.src.BaseSrcGenContext;

import java.util.Map;
import java.util.Set;

final class SrcGenContext extends BaseSrcGenContext {

  private final ProgramNode program;

  SrcGenContext(final ProgramNode program,
                final Map<String, String> externalTypes,
                final Set<String> imports,
                final Set<String> staticImports,
                final String tab,
                final String srcPackage,
                final String commonsPackage,
                final String typePackage,
                final String programName) {
    super(externalTypes, imports, staticImports, tab, srcPackage, commonsPackage, typePackage, programName);
    this.program = program;
  }

  DefinedTypeNode definedType(final String name) {
    return program.definedType(name);
  }

  AccountNode account(final String name) {
    return program.account(name);
  }

  public ProgramNode program() {
    return program;
  }
}
