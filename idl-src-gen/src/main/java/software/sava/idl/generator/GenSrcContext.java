package software.sava.idl.generator;

import software.sava.core.accounts.PublicKey;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public record GenSrcContext(IDLType idlType,
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

  private static String getPackageGroup(final String importLine) {
    int i = importLine.indexOf('.');
    if (i < 0) {
      return importLine;
    } else {
      i = importLine.indexOf('.', i + 1);
      return i < 0 ? importLine : importLine.substring(0, i);
    }
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

  public int tabLength() {
    return tab.length();
  }

  public void appendPackage(final StringBuilder builder) {
    builder.append("package ").append(srcPackage).append(";\n\n");
  }

  public void clearImports() {
    imports.clear();
    staticImports.clear();
  }

  public boolean isExternalType(final String typeName) {
    return externalTypes.containsKey(typeName);
  }

  public void addDefinedImport(final String className) {
    final var externalType = externalTypes.get(className);
    imports.add(externalType != null ? externalType : String.format("%s.%s", typePackage, className));
  }

  public void addImportIfExternal(final String typeName) {
    final var externalType = externalTypes.get(typeName);
    if (externalType != null) {
      imports.add(externalType);
    }
  }

  public void addImport(final String className) {
    imports.add(className);
  }

  public void importCommons(final String simpleClassNane) {
    imports.add(commonsPackage + '.' + simpleClassNane);
  }

  public void addImport(final Class<?> clas) {
    addImport(clas.getName());
  }

  public void addStaticImport(final String className) {
    staticImports.add(className);
  }

  public void addStaticImport(final Class<?> clas, final String constantName) {
    addStaticImport(clas.getName() + '.' + constantName);
  }

  public void addUTF_8Import() {
    addStaticImport(StandardCharsets.class, "UTF_8");
  }

  public void addUS_ASCII_Import() {
    addStaticImport(StandardCharsets.class, "US_ASCII");
  }

  public boolean appendImports(final StringBuilder builder) {
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
}
