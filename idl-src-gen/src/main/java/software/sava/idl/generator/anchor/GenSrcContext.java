package software.sava.idl.generator.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.idl.generator.src.BaseSrcGenContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class GenSrcContext extends BaseSrcGenContext {

  private final IDLType idlType;
  private final boolean accountsHaveDiscriminators;
  private final Set<String> accounts;
  private final Map<String, NamedType> definedTypes;
  private final Map<PublicKey, AccountReferenceCall> accountMethods;

  public GenSrcContext(IDLType idlType,
                       boolean accountsHaveDiscriminators,
                       Set<String> accounts,
                       Map<String, String> externalTypes,
                       Map<String, NamedType> definedTypes,
                       Set<String> imports,
                       Set<String> staticImports,
                       String tab,
                       String srcPackage,
                       String commonsPackage,
                       String typePackage,
                       String programName,
                       Map<PublicKey, AccountReferenceCall> accountMethods) {
    super(externalTypes, imports, staticImports, tab, srcPackage, commonsPackage, typePackage, programName);
    this.idlType = idlType;
    this.accountsHaveDiscriminators = accountsHaveDiscriminators;
    this.accounts = accounts;
    this.definedTypes = definedTypes;
    this.accountMethods = accountMethods;
  }

  public boolean isAccount(final String typeName) {
    return accounts.contains(typeName);
  }

  public boolean hasDiscriminator(final boolean isAccount, final boolean hasDiscriminator) {
    if (isAccount) {
      return accountsHaveDiscriminators;
    } else {
      return hasDiscriminator;
    }
  }

  public boolean hasDiscriminator(final boolean isAccount) {
    return isAccount && accountsHaveDiscriminators;
  }

  public void addDefinedImport(final String className) {
    final var externalType = externalTypes.get(className);
    imports.add(externalType != null ? externalType : String.format("%s.%s", typePackage, className));
  }

  public IDLType idlType() {
    return idlType;
  }

  public boolean accountsHaveDiscriminators() {
    return accountsHaveDiscriminators;
  }

  public Set<String> accounts() {
    return accounts;
  }

  public Map<String, NamedType> definedTypes() {
    return definedTypes;
  }

  public Map<PublicKey, AccountReferenceCall> accountMethods() {
    return accountMethods;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) return true;
    if (obj == null || obj.getClass() != this.getClass()) return false;
    var that = (GenSrcContext) obj;
    return Objects.equals(this.idlType, that.idlType) &&
        this.accountsHaveDiscriminators == that.accountsHaveDiscriminators &&
        Objects.equals(this.accounts, that.accounts) &&
        Objects.equals(this.externalTypes, that.externalTypes) &&
        Objects.equals(this.definedTypes, that.definedTypes) &&
        Objects.equals(this.imports, that.imports) &&
        Objects.equals(this.staticImports, that.staticImports) &&
        Objects.equals(this.tab, that.tab) &&
        Objects.equals(this.srcPackage, that.srcPackage) &&
        Objects.equals(this.commonsPackage, that.commonsPackage) &&
        Objects.equals(this.typePackage, that.typePackage) &&
        Objects.equals(this.programName, that.programName) &&
        Objects.equals(this.accountMethods, that.accountMethods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(idlType, accountsHaveDiscriminators, accounts, externalTypes, definedTypes, imports, staticImports, tab, srcPackage, commonsPackage, typePackage, programName, accountMethods);
  }

  @Override
  public String toString() {
    return "GenSrcContext[" +
        "idlType=" + idlType + ", " +
        "accountsHaveDiscriminators=" + accountsHaveDiscriminators + ", " +
        "accounts=" + accounts + ", " +
        "externalTypes=" + externalTypes + ", " +
        "definedTypes=" + definedTypes + ", " +
        "imports=" + imports + ", " +
        "staticImports=" + staticImports + ", " +
        "tab=" + tab + ", " +
        "srcPackage=" + srcPackage + ", " +
        "commonsPackage=" + commonsPackage + ", " +
        "typePackage=" + typePackage + ", " +
        "programName=" + programName + ", " +
        "accountMethods=" + accountMethods + ']';
  }

}
