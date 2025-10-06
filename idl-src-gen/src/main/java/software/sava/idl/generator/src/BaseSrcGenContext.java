package software.sava.idl.generator.src;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

public abstract class BaseSrcGenContext {

  protected final Map<String, String> externalTypes;
  protected final Set<String> imports;
  protected final Set<String> staticImports;
  protected final String tab;
  protected final String srcPackage;
  protected final String commonsPackage;
  protected final String typePackage;
  protected final String programName;

  protected BaseSrcGenContext(Map<String, String> externalTypes,
                              Set<String> imports,
                              Set<String> staticImports,
                              String tab,
                              String srcPackage,
                              String commonsPackage,
                              String typePackage,
                              String programName) {
    this.externalTypes = externalTypes;
    this.imports = imports;
    this.staticImports = staticImports;
    this.tab = tab;
    this.srcPackage = srcPackage;
    this.commonsPackage = commonsPackage;
    this.typePackage = typePackage;
    this.programName = programName;
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

  public final int tabLength() {
    return tab.length();
  }

  public final void appendPackage(final StringBuilder builder) {
    builder.append("package ").append(srcPackage).append(";\n\n");
  }

  public final void clearImports() {
    imports.clear();
    staticImports.clear();
  }

  public final boolean isExternalType(final String typeName) {
    return externalTypes.containsKey(typeName);
  }

  public final void addImportIfExternal(final String typeName) {
    final var externalType = externalTypes.get(typeName);
    if (externalType != null) {
      imports.add(externalType);
    }
  }

  public final void addImport(final String className) {
    imports.add(className);
  }

  public final void importCommons(final String simpleClassNane) {
    imports.add(commonsPackage + '.' + simpleClassNane);
  }

  public final void addImport(final Class<?> clas) {
    addImport(clas.getName());
  }

  public final void addStaticImport(final String className) {
    staticImports.add(className);
  }

  public final void addStaticImport(final Class<?> clas, final String constantName) {
    addStaticImport(clas.getName() + '.' + constantName);
  }

  public final void addUTF_8Import() {
    addStaticImport(StandardCharsets.class, "UTF_8");
  }

  public final void addUS_ASCII_Import() {
    addStaticImport(StandardCharsets.class, "US_ASCII");
  }

  public final boolean appendImports(final StringBuilder builder) {
    if (imports.isEmpty() && staticImports.isEmpty()) {
      return false;
    }

    String group, currentGroup = null;
    for (final var importLine : imports) {
      group = BaseSrcGenContext.getPackageGroup(importLine);
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
        group = BaseSrcGenContext.getPackageGroup(importLine);
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

  public final Map<String, String> externalTypes() {
    return externalTypes;
  }

  public final Set<String> imports() {
    return imports;
  }

  public final Set<String> staticImports() {
    return staticImports;
  }

  public final String tab() {
    return tab;
  }

  public final String srcPackage() {
    return srcPackage;
  }

  public final String commonsPackage() {
    return commonsPackage;
  }

  public final String typePackage() {
    return typePackage;
  }

  public final String programName() {
    return programName;
  }
}
