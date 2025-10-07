package software.sava.idl.generator.src;

import software.sava.core.accounts.PublicKey;
import software.sava.idl.generator.anchor.AccountReferenceCall;
import software.sava.idl.generator.anchor.SrcGenContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public abstract class BaseSrcGenContext implements SrcGenContext {

  protected final Map<String, String> externalTypes;
  protected final Set<String> imports;
  protected final Set<String> staticImports;
  protected final String tab;
  protected final String srcPackage;
  protected final String commonsPackage;
  protected final String typePackage;
  protected final String programName;
  protected final Set<String> accounts;
  protected final Map<PublicKey, AccountReferenceCall> accountMethods;

  protected BaseSrcGenContext(final Map<String, String> externalTypes,
                              final Set<String> imports,
                              final Set<String> staticImports,
                              final String tab,
                              final String srcPackage,
                              final String commonsPackage,
                              final String typePackage,
                              final String programName,
                              final Set<String> accounts,
                              final Map<PublicKey, AccountReferenceCall> accountMethods) {
    this.externalTypes = externalTypes;
    this.imports = imports;
    this.staticImports = staticImports;
    this.tab = tab;
    this.srcPackage = srcPackage;
    this.commonsPackage = commonsPackage;
    this.typePackage = typePackage;
    this.programName = programName;
    this.accounts = accounts;
    this.accountMethods = accountMethods;
  }

  @Override
  public final int tabLength() {
    return tab.length();
  }

  @Override
  public final void appendPackage(final StringBuilder builder) {
    builder.append("package ").append(srcPackage).append(";\n\n");
  }

  @Override
  public final void clearImports() {
    imports.clear();
    staticImports.clear();
  }

  @Override
  public final boolean isExternalType(final String typeName) {
    return externalTypes.containsKey(typeName);
  }

  @Override
  public final void addImportIfExternal(final String typeName) {
    final var externalType = externalTypes.get(typeName);
    if (externalType != null) {
      imports.add(externalType);
    }
  }

  @Override
  public final void addImport(final String className) {
    imports.add(className);
  }

  @Override
  public final void importCommons(final String simpleClassNane) {
    imports.add(commonsPackage + '.' + simpleClassNane);
  }

  @Override
  public final void addImport(final Class<?> clas) {
    addImport(clas.getName());
  }

  @Override
  public final void addStaticImport(final String className) {
    staticImports.add(className);
  }

  @Override
  public final void addStaticImport(final Class<?> clas, final String constantName) {
    addStaticImport(clas.getName() + '.' + constantName);
  }

  @Override
  public final void addUTF_8Import() {
    addStaticImport(StandardCharsets.class, "UTF_8");
  }

  @Override
  public final void addUS_ASCII_Import() {
    addStaticImport(StandardCharsets.class, "US_ASCII");
  }

  private static String getPackageGroup(final String importLine) {
    int i = importLine.indexOf('.');
    if (i < 0) {
      return importLine;
    } else {
      i = importLine.indexOf('.', i + 1);
      return i < 0 ? importLine : importLine.substring(0, i);
    }
  }

  @Override
  public final boolean appendImports(final StringBuilder builder) {
    if (imports.isEmpty() && staticImports.isEmpty()) {
      return false;
    }

    String group, currentGroup = null;
    for (final var importLine : imports) {
      group = getPackageGroup(importLine);
      if (currentGroup == null) {
        currentGroup = group;
      } else if (!group.equals(currentGroup)) {
        builder.append('\n');
        currentGroup = group;
      }
      builder.append("import ").append(importLine).append(";\n");
    }
    if (!staticImports.isEmpty()) {
      currentGroup = null;
      builder.append('\n');
      for (final var importLine : staticImports) {
        group = getPackageGroup(importLine);
        if (currentGroup == null) {
          currentGroup = group;
        } else if (!group.equals(currentGroup)) {
          builder.append('\n');
          currentGroup = group;
        }
        builder.append("import static ").append(importLine).append(";\n");
      }
    }
    return true;
  }

  @Override
  public final String tab() {
    return tab;
  }

  @Override
  public final String typePackage() {
    return typePackage;
  }

  @Override
  public final String programName() {
    return programName;
  }

  public final Set<String> accounts() {
    return accounts;
  }

  @Override
  public final boolean isAccount(final String typeName) {
    return accounts.contains(typeName);
  }

  @Override
  public final Map<PublicKey, AccountReferenceCall> accountMethods() {
    return accountMethods;
  }

  @Override
  public final void addDefinedImport(final String className) {
    final var externalType = externalTypes.get(className);
    imports.add(externalType != null ? externalType : String.format("%s.%s", typePackage, className));
  }
}
