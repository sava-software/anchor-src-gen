package software.sava.anchor;

import software.sava.core.accounts.PublicKey;
import software.sava.core.borsh.Borsh;
import software.sava.core.programs.Discriminator;
import software.sava.core.rpc.Filter;
import software.sava.rpc.json.http.response.AccountInfo;
import systems.comodal.jsoniter.JsonIterator;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;
import static software.sava.anchor.AnchorInstruction.replaceNewLinesIfLessThan;
import static software.sava.anchor.AnchorNamedTypeParser.parseLowerList;
import static software.sava.anchor.AnchorSourceGenerator.removeBlankLines;
import static software.sava.anchor.AnchorType.string;
import static software.sava.core.rpc.Filter.MAX_MEM_COMP_LENGTH;

public record AnchorStruct(List<NamedType> fields) implements AnchorDefinedTypeContext {

  private static final String LENGTH_ADD_ALIGN_TAB = " ".repeat("retur".length());

  static AnchorStruct parseStruct(final IDLType idlType, final JsonIterator ji) {
    final var fields = parseLowerList(idlType, ji);
    return new AnchorStruct(fields);
  }

  static String generateRecord(final GenSrcContext genSrcContext,
                               final NamedType context,
                               final List<NamedType> fields,
                               final String recordAccessModifier,
                               final String interfaceName,
                               final int ordinal) {
    return generateRecord(genSrcContext, context, fields, recordAccessModifier, interfaceName, ordinal, false, null, false);
  }

  static String generateRecord(final GenSrcContext genSrcContext,
                               final NamedType context,
                               final List<NamedType> fields,
                               final String recordAccessModifier,
                               final String interfaceName,
                               final int ordinal,
                               final boolean isAccount,
                               final NamedType account,
                               final boolean hasDiscriminator) {
    final var tab = genSrcContext.tab();
    final int tabLength = tab.length();
    final var builder = new StringBuilder(4_096);
    final var paramsBuilder = new StringBuilder(4_096);

    final var name = context.name();
    final var recordSigLine = recordAccessModifier
        + (recordAccessModifier.isBlank() ? "" : " ")
        + String.format("record %s(", name);

    if (fields.isEmpty()) {
      builder.append(context.docComments());
      builder.append(recordSigLine).append(String.format("""
          ) implements %s {
          
          """, interfaceName
      ));
      builder.append(String.format("""
          private static final %s INSTANCE = new %s();
          
          public static %s read(final byte[] _data, final int offset) {
          %sreturn INSTANCE;
          }
          
          @Override
          public int write(final byte[] _data, final int offset) {
          %sreturn 0;
          }
          
          @Override
          public int l() {
          %sreturn 0;
          }
          """, name, name, name, tab, tab, tab
      ).indent(tabLength));
      return removeBlankLines(builder.append('}').toString());
    }

    final StringBuilder offsetsBuilder;
    final StringBuilder memCompFiltersBuilder;
    if (isAccount) {
      paramsBuilder.append("PublicKey _address,\n");
      paramsBuilder.append("Discriminator discriminator,\n");
      genSrcContext.addImport(Discriminator.class);
      offsetsBuilder = new StringBuilder(2_048);
      memCompFiltersBuilder = new StringBuilder(4_096);
    } else {
      if (hasDiscriminator) {
        paramsBuilder.append("Discriminator discriminator,\n");
      }
      offsetsBuilder = null;
      memCompFiltersBuilder = null;
    }
    int byteLength = hasDiscriminator ? AnchorUtil.DISCRIMINATOR_LENGTH : 0;
    var fieldIterator = fields.iterator();
    for (NamedType field; ; ) {
      field = fieldIterator.next();
      if (byteLength >= 0) {
        final var type = field.type();
        if (offsetsBuilder == null) {
          if (type.isFixedLength(genSrcContext.definedTypes())) {
            byteLength += type.serializedLength(genSrcContext);
          } else {
            byteLength = -1;
          }
        } else {
          final var offsetVarName = AnchorUtil.snakeCase(field.name()).toUpperCase(ENGLISH) + "_OFFSET";
          offsetsBuilder.append(String.format("""
                  public static final int %s = %d;
                  """,
              offsetVarName, byteLength
          ));
          if (type.isFixedLength(genSrcContext.definedTypes())) {
            final int serializedLength = type.serializedLength(genSrcContext);
            if (serializedLength <= MAX_MEM_COMP_LENGTH) {
              field.generateMemCompFilter(genSrcContext, memCompFiltersBuilder, offsetVarName);
            }
            byteLength += serializedLength;
          } else {
            final int serializedLength = type.fixedSerializedLength(genSrcContext);
            if (serializedLength > 0 && serializedLength <= MAX_MEM_COMP_LENGTH) {
              field.generateMemCompFilter(genSrcContext, memCompFiltersBuilder, offsetVarName);
            }
            byteLength = -1;
          }
        }
      }
      paramsBuilder.append(field.generateRecordField(genSrcContext));
      if (fieldIterator.hasNext()) {
        paramsBuilder.append(",\n");
      } else {
        break;
      }
    }

    final var params = paramsBuilder.toString().indent(recordSigLine.length()).strip();

    builder
        .append(context.docComments())
        .append(recordSigLine)
        .append(replaceNewLinesIfLessThan(params, fields.size(), 3))
        .append(String.format("""
            ) implements %s {
            
            """, interfaceName
        ));
    if (byteLength > 0) {
      builder.append(tab).append(String.format("""
              public static final int BYTES = %d;
              """,
          byteLength
      ));
    }
    for (final var field : fields) {
      final var fixedArrayLength = field.arrayLengthConstant();
      if (fixedArrayLength != null) {
        builder.append(tab).append(fixedArrayLength);
      }
    }
    if (isAccount) {
      if (byteLength > 0) {
        builder.append(tab).append("""
            public static final Filter SIZE_FILTER = Filter.createDataSizeFilter(BYTES);
            
            """);
        genSrcContext.addImport(Filter.class);
      }

      final var discriminator = account.discriminator();
      if (discriminator != null) {
        genSrcContext.addImport(Filter.class);
        genSrcContext.addImport(Discriminator.class);
        genSrcContext.addStaticImport(Discriminator.class, "toDiscriminator");

        final var discriminatorLine = Arrays.stream(discriminator.toIntArray())
            .mapToObj(Integer::toString)
            .collect(Collectors.joining(", ",
                "public static final Discriminator DISCRIMINATOR = toDiscriminator(", ");"
            ));
        builder.append(tab).append(discriminatorLine);
        builder.append("\n").append(tab).append("""
            public static final Filter DISCRIMINATOR_FILTER = Filter.createMemCompFilter(0, DISCRIMINATOR.data());
            
            """);
      }

      if (!offsetsBuilder.isEmpty()) {
        builder.append(offsetsBuilder.toString().indent(tabLength));
        builder.append(memCompFiltersBuilder.toString().indent(tabLength)).append('\n');
      }
    } else if (byteLength > 0) {
      builder.append('\n');
    }

    final var returnNewLine = String.format("return new %s(", name);
    if (fields.stream().anyMatch(namedType -> namedType.type().type() == string)) {
      final var factoryMethodBuilder = new StringBuilder(2_048);
      if (isAccount) {
        factoryMethodBuilder.append("final PublicKey _address,\n");
      }
      if (hasDiscriminator) {
        factoryMethodBuilder.append("final Discriminator discriminator,\n");
      }
      fieldIterator = fields.iterator();
      for (NamedType field; ; ) {
        field = fieldIterator.next();
        factoryMethodBuilder.append("final ").append(field.generateStaticFactoryField(genSrcContext));
        if (fieldIterator.hasNext()) {
          factoryMethodBuilder.append(",\n");
        } else {
          break;
        }
      }

      final var staticFactoryLine = String.format("public static %s createRecord(", name);
      final var staticFactoryParams = factoryMethodBuilder.toString().indent(staticFactoryLine.length() + tabLength).strip();
      builder.append(tab).append(staticFactoryLine);
      builder.append(replaceNewLinesIfLessThan(staticFactoryParams, fields.size(), 3))
          .append("""
              ) {
              """);

      final var newInstanceBuilder = new StringBuilder(2_048);
      if (isAccount) {
        newInstanceBuilder.append("_address,\n");
      }
      if (hasDiscriminator) {
        newInstanceBuilder.append("discriminator,\n");
      }
      fieldIterator = fields.iterator();
      for (NamedType field; ; ) {
        field = fieldIterator.next();
        newInstanceBuilder.append(field.generateNewInstanceField(genSrcContext));
        if (fieldIterator.hasNext()) {
          newInstanceBuilder.append(",\n");
        } else {
          break;
        }
      }
      final var newInstanceParams = newInstanceBuilder.toString().indent(returnNewLine.length() + tabLength + tabLength).strip();
      builder
          .append(tab).append(tab).append(returnNewLine)
          .append(replaceNewLinesIfLessThan(newInstanceParams, fields.size(), 4))
          .append(");\n")
          .append(tab).append("}\n\n");
    }

    final var readBuilder = new StringBuilder(4_096);
    final boolean singleField = !hasDiscriminator && fields.size() == 1;
    final var offsetVarName = singleField ? "offset" : "i";
    fieldIterator = fields.iterator();
    for (NamedType field; ; ) {
      field = fieldIterator.next();
      final boolean hasNext = fieldIterator.hasNext();
      readBuilder.append(field.generateRead(genSrcContext, hasNext, singleField, offsetVarName)).append('\n');
      if (!hasNext) {
        break;
      }
    }
    builder.append(String.format("public static %s read(final byte[] _data, final int offset) {", name).indent(tabLength));
    if (isAccount) {
      genSrcContext.addImport(AccountInfo.class);
      builder.append(String.format("""
              %sreturn read(null, _data, offset);
              }
              
              public static %s read(final AccountInfo<byte[]> accountInfo) {
              %sreturn read(accountInfo.pubKey(), accountInfo.data(), 0);
              }
              
              public static %s read(final PublicKey _address, final byte[] _data) {
              %sreturn read(_address, _data, 0);
              }
              
              public static final BiFunction<PublicKey, byte[], %s> FACTORY = %s::read;
              
              public static %s read(final PublicKey _address, final byte[] _data, final int offset) {""",
          tab, name, tab, name, tab, name, name, name
      ).indent(tabLength));
      builder.append("""
          if (_data == null || _data.length == 0) {""".indent(tabLength << 1));
      builder.append(tab).append("""
          return null;
          }
          final var discriminator = parseDiscriminator(_data, offset);
          int i = offset + discriminator.length();""".indent(tabLength << 1)
      );
      genSrcContext.addImport(BiFunction.class);
      genSrcContext.addImport(PublicKey.class);
      genSrcContext.addStaticImport(AnchorUtil.class, "parseDiscriminator");
    } else {
      builder.append("""
          if (_data == null || _data.length == 0) {""".indent(tabLength << 1));
      builder.append(tab).append("""
          return null;
          }""".indent(tabLength << 1)
      );
      if (hasDiscriminator) {
        builder.append("""
            final var discriminator = parseDiscriminator(_data, offset);
            int i = offset + discriminator.length();""".indent(tabLength << 1)
        );
        genSrcContext.addStaticImport(AnchorUtil.class, "parseDiscriminator");
      } else if (!singleField) {
        builder.append(tab).append(tab).append("int i = offset;\n");
      }
    }
    builder.append(readBuilder.toString().indent(tabLength << 1));
    final var newInstanceBuilder = new StringBuilder(2_048);
    if (isAccount) {
      newInstanceBuilder.append("_address,\n");
    }
    if (hasDiscriminator) {
      newInstanceBuilder.append("discriminator,\n");
    }
    fieldIterator = fields.iterator();
    for (NamedType field; ; ) {
      field = fieldIterator.next();
      newInstanceBuilder.append(field.generateNewInstanceField(genSrcContext));
      if (fieldIterator.hasNext()) {
        newInstanceBuilder.append(",\n");
      } else {
        break;
      }
    }
    final var newInstanceParams = newInstanceBuilder.toString().indent(returnNewLine.length() + tabLength + tabLength).strip();
    builder
        .append(tab).append(tab).append(returnNewLine)
        .append(replaceNewLinesIfLessThan(newInstanceParams, fields.size(), 4))
        .append(");\n")
        .append(tab).append("}\n\n");


    final var writeBuilder = new StringBuilder(4_096);
    for (final var field : fields) {
      writeBuilder.append(field.generateWrite(genSrcContext, true)).append('\n');
    }
    builder.append("""
        @Override
        public int write(final byte[] _data, final int offset) {
        """.indent(tabLength));
    if (ordinal < 0) {
      if (hasDiscriminator) {
        builder.append("""
            int i = offset + discriminator.write(_data, offset);""".indent(tabLength << 1));
      } else {
        builder.append(tab).append(tab).append("int i = offset;\n");
      }
    } else {
      builder.append(tab).append(tab).append("int i = writeOrdinal(_data, offset);\n");
    }
    builder.append(writeBuilder.toString().indent(tabLength << 1));
    builder.append(tab).append(tab).append("return i - offset;\n");
    builder.append(tab).append("}\n\n");

    final var lengthBuilder = new StringBuilder(4_096);
    fieldIterator = fields.iterator();
    for (NamedType field; ; ) {
      field = fieldIterator.next();
      lengthBuilder.append(field.generateLength(genSrcContext));
      if (fieldIterator.hasNext()) {
        lengthBuilder.append('\n').append(tab).append(tab).append(LENGTH_ADD_ALIGN_TAB).append("+ ");
      } else {
        break;
      }
    }

    if (byteLength > 0) {
      builder.append(String.format("""
                  @Override
                  public int l() {
                  %sreturn BYTES;
                  }""",
              tab
          ).indent(tabLength)
      );
    } else {
      builder.append("""
          @Override
          public int l() {
          """.indent(tabLength));
      builder.append(tab).append(tab);
      if (ordinal < 0) {
        if (hasDiscriminator) {
          builder.append(String.format("return %d + ", AnchorUtil.DISCRIMINATOR_LENGTH));
        } else {
          builder.append("return ");
        }
      } else {
        builder.append("return 1 + ");
      }
      builder.append(replaceNewLinesIfLessThan(lengthBuilder, fields.size(), 5)).append(";\n");
      builder.append(tab).append("}\n");
    }

    if (ordinal >= 0) {
      builder.append(String.format("""
          
          @Override
          public int ordinal() {
            return %d;
          }
          """, ordinal
      ).indent(tabLength));
    }

    return removeBlankLines(builder.append('}').toString());
  }

  static String generatePublicRecord(final GenSrcContext genSrcContext,
                                     final NamedType context,
                                     final List<NamedType> fields,
                                     final boolean isAccount,
                                     final NamedType account,
                                     final boolean hasDiscriminator) {
    return generateRecord(genSrcContext, context, fields, "public", "Borsh", -1, isAccount, account, hasDiscriminator);
  }

  @Override
  public AnchorType type() {
    return AnchorType.struct;
  }

  @Override
  public int numElements() {
    return fields.size();
  }

  @Override
  public boolean isFixedLength(final Map<String, NamedType> definedTypes) {
    return fields.stream()
        .map(NamedType::type)
        .allMatch(type -> type.isFixedLength(definedTypes));
  }

  @Override
  public int serializedLength(final GenSrcContext genSrcContext, final boolean hasDiscriminator) {
    int serializedLength = hasDiscriminator ? AnchorUtil.DISCRIMINATOR_LENGTH : 0;
    int len;
    for (final var field : fields) {
      final var type = field.type();
      len = field.type().serializedLength(genSrcContext, genSrcContext.isAccount(type.typeName()));
      if (len <= 0) {
//        throw throwInvalidDataType();
        return len;
      } else {
        serializedLength += len;
      }
    }
    return serializedLength;
  }

  @Override
  public int fixedSerializedLength(final GenSrcContext genSrcContext, final boolean hasDiscriminator) {
    final var definedTypes = genSrcContext.definedTypes();
    int serializedLength = hasDiscriminator ? AnchorUtil.DISCRIMINATOR_LENGTH : 0;
    for (final var field : fields) {
      if (isFixedLength(definedTypes)) {
        final var type = field.type();
        serializedLength += type.serializedLength(genSrcContext, genSrcContext.isAccount(type.typeName()));
      } else {
        return serializedLength;
      }
    }
    return serializedLength;
  }

  public String generateSource(final GenSrcContext genSrcContext, final NamedType context) {
    final var builder = new StringBuilder(4_096);
    genSrcContext.addImport(Borsh.class);
    final var recordSource = generatePublicRecord(genSrcContext, context, fields, false, null, true);
    return builder.append('\n').append(recordSource).toString();
  }

  public String generateSource(final GenSrcContext genSrcContext,
                               final String packageName,
                               final NamedType context,
                               final boolean isAccount,
                               final NamedType account) {
    final var builder = new StringBuilder(4_096);
    builder.append("package ").append(packageName).append(";\n\n");

    genSrcContext.addImport(Borsh.class);

    final var recordSource = generatePublicRecord(genSrcContext, context, fields, isAccount, account, isAccount);

    genSrcContext.appendImports(builder);

    return builder.append('\n').append(recordSource).toString();
  }
}
