package software.sava.idl.generator.codama;

import software.sava.core.accounts.PublicKey;
import software.sava.idl.generator.anchor.AccountReferenceCall;
import software.sava.idl.generator.anchor.IDLType;
import software.sava.idl.generator.src.BaseSrcGenContext;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

final class CodamaSrcGenContext extends BaseSrcGenContext {

  private final ProgramNode program;

  CodamaSrcGenContext(final ProgramNode program,
                      final Map<String, String> externalTypes,
                      final Set<String> imports,
                      final Set<String> staticImports,
                      final String tab,
                      final String srcPackage,
                      final String commonsPackage,
                      final String typePackage,
                      final String programName,
                      final Set<String> accounts,
                      final Map<PublicKey, AccountReferenceCall> accountMethods) {
    super(
        externalTypes,
        imports,
        staticImports,
        tab,
        srcPackage,
        commonsPackage,
        typePackage,
        programName,
        accounts,
        accountMethods
    );
    this.program = program;
  }

  static CodamaSrcGenContext createContext(final ProgramNode program,
                                           final Map<String, String> externalTypes,
                                           final Set<String> imports,
                                           final Set<String> staticImports,
                                           final String tab,
                                           final String srcPackage,
                                           final String commonsPackage,
                                           final String typePackage,
                                           final String programName,
                                           final Map<PublicKey, AccountReferenceCall> accountMethods) {
    final var accounts = program.accounts().stream().map(AccountNode::name).collect(Collectors.toUnmodifiableSet());
    return new CodamaSrcGenContext(
        program,
        externalTypes,
        imports,
        staticImports,
        tab,
        srcPackage,
        commonsPackage,
        typePackage,
        programName,
        accounts,
        accountMethods
    );
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

  @Override
  public IDLType idlType() {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public boolean hasDiscriminator(final boolean isAccount, final boolean hasDiscriminator) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public boolean hasDiscriminator(final boolean isAccount) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public boolean isDefinedType(final String typeName) {
    return program.isDefinedType(typeName);
  }

  @Override
  public boolean isDefinedTypeFixedLength(final String typeName) {
    throw new UnsupportedOperationException("Not implemented yet");
  }

  @Override
  public int definedTypeSerializedLength(final String typeName) {
    throw new UnsupportedOperationException("Not implemented yet");
  }
}
