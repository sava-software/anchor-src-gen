package software.sava.idl.generator.anchor;

import software.sava.core.accounts.PublicKey;

import java.util.Map;

public interface SrcGenContext {

  IDLType idlType();

  boolean hasDiscriminator(final boolean isAccount, final boolean hasDiscriminator);

  boolean hasDiscriminator(final boolean isAccount);

  boolean isDefinedType(final String typeName);

  boolean isDefinedTypeFixedLength(final String typeName);

  int definedTypeSerializedLength(final String typeName);

  int tabLength();

  void appendPackage(final StringBuilder builder);

  void clearImports();

  boolean isExternalType(final String typeName);

  void addImportIfExternal(final String typeName);

  void addImport(final String className);

  void importCommons(final String simpleClassNane);

  void addImport(final Class<?> clas);

  void addStaticImport(final String className);

  void addStaticImport(final Class<?> clas, final String constantName);

  void addUTF_8Import();

  void addUS_ASCII_Import();

  boolean appendImports(final StringBuilder builder);

  String tab();

  String typePackage();

  String programName();

  boolean isAccount(final String typeName);

  Map<PublicKey, AccountReferenceCall> accountMethods();

  void addDefinedImport(final String className);
}
