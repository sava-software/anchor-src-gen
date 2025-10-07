package software.sava.idl.generator.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.idl.generator.src.BaseSrcGenContext;

import java.util.Map;
import java.util.Set;

final class AnchorSrcGenContext extends BaseSrcGenContext implements SrcGenContext {

  private final IDLType idlType;
  private final boolean accountsHaveDiscriminators;
  private final Map<String, NamedType> definedTypes;

  AnchorSrcGenContext(final IDLType idlType,
                      final boolean accountsHaveDiscriminators,
                      final Set<String> accounts,
                      final Map<String, String> externalTypes,
                      final Map<String, NamedType> definedTypes,
                      final Set<String> imports,
                      final Set<String> staticImports,
                      final String tab,
                      final String srcPackage,
                      final String commonsPackage,
                      final String typePackage,
                      final String programName,
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
    this.idlType = idlType;
    this.accountsHaveDiscriminators = accountsHaveDiscriminators;
    this.definedTypes = definedTypes;
  }

  @Override
  public IDLType idlType() {
    return idlType;
  }

  @Override
  public boolean hasDiscriminator(final boolean isAccount, final boolean hasDiscriminator) {
    if (isAccount) {
      return accountsHaveDiscriminators;
    } else {
      return hasDiscriminator;
    }
  }

  @Override
  public boolean hasDiscriminator(final boolean isAccount) {
    return isAccount && accountsHaveDiscriminators;
  }

  @Override
  public boolean isDefinedType(final String typeName) {
    return definedTypes.containsKey(typeName);
  }

  @Override
  public boolean isDefinedTypeFixedLength(final String typeName) {
    final var definedType = definedTypes.get(typeName);
    if (definedType == null) {
      throw new IllegalStateException("Failed to find defined type " + typeName);
    }
    return definedType.type().isFixedLength(this);
  }

  @Override
  public int definedTypeSerializedLength(final String typeName) {
    return definedTypes.get(typeName).type().serializedLength(this, isAccount(typeName));
  }
}
