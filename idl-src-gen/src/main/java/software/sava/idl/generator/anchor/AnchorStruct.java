package software.sava.idl.generator.anchor;

import software.sava.anchor.AnchorUtil;
import software.sava.core.borsh.Borsh;
import software.sava.core.programs.Discriminator;
import software.sava.core.rpc.Filter;
import software.sava.idl.generator.src.BaseStruct;
import software.sava.idl.generator.src.NamedType;
import software.sava.idl.generator.src.StructGen;
import systems.comodal.jsoniter.JsonIterator;

import java.util.HashMap;
import java.util.List;

import static java.util.Locale.ENGLISH;
import static software.sava.core.rpc.Filter.MAX_MEM_COMP_LENGTH;
import static software.sava.idl.generator.ParseUtil.removeBlankLines;
import static software.sava.idl.generator.anchor.AnchorNamedTypeParser.parseLowerList;
import static software.sava.idl.generator.src.SrcUtil.replaceNewLinesIfLessThan;

final class AnchorStruct extends BaseStruct<NamedType> implements AnchorDefinedTypeContext {

  AnchorStruct(final List<NamedType> fields) {
    super(deduplicate(fields));
  }

  static List<NamedType> deduplicate(final List<NamedType> types) {
    final var deDuplicate = HashMap.<String, Integer>newHashMap(types.size());
    return types.stream().map(type -> {
      final var name = type.name();
      final int dedupeCount = deDuplicate.compute(name, (_, v) -> v == null ? 1 : v + 1);
      return dedupeCount == 1 ? type : type.rename(name + dedupeCount);
    }).toList();
  }

  static AnchorStruct parseStruct(final IDLType idlType, final JsonIterator ji) {
    final var fields = parseLowerList(idlType, ji);
    return new AnchorStruct(fields);
  }

  @Override
  protected Discriminator appendAccountDiscriminator(final SrcGenContext srcGenContext,
                                                     final StringBuilder builder,
                                                     final NamedType account) {
    final var accountDiscriminator = account.discriminator();
    if (accountDiscriminator != null) {
      appendAccountDiscriminator(srcGenContext, builder, accountDiscriminator, 0);
    }
    return accountDiscriminator;
  }

  @Override
  protected boolean hasStringFields() {
    return fields.stream().anyMatch(namedType -> namedType.type().isString());
  }

  String generateRecord(final SrcGenContext srcGenContext,
                        final NamedType context,
                        final String interfaceName,
                        final int ordinal) {
    return generateRecord(srcGenContext, context, false, interfaceName, ordinal, false, null, false);
  }

  private static final boolean TEST_SUPER = false;

  protected String generateRecord(final SrcGenContext srcGenContext,
                                  final NamedType context,
                                  final boolean publicAccess,
                                  final String interfaceName,
                                  final int ordinal,
                                  final boolean isAccount,
                                  final NamedType account,
                                  final boolean maybeHasDiscriminator) {
    if (TEST_SUPER) {
      return super.generateRecord(srcGenContext, context, publicAccess, interfaceName, ordinal, isAccount, account, maybeHasDiscriminator);
    } else {
      final boolean hasDiscriminator = srcGenContext.hasDiscriminator(isAccount, maybeHasDiscriminator);
      final var tab = srcGenContext.tab();
      final int tabLength = tab.length();
      final var builder = new StringBuilder(4_096);
      builder.append(context.docComments());

      final var name = context.name();
      final int recordSigLineLength = StructGen.sigLine(builder, name, publicAccess);

      if (fields.isEmpty()) {
        return StructGen.emptyStruct(tab, builder, interfaceName, name);
      }

      final var paramsBuilder = new StringBuilder(4_096);
      final StringBuilder offsetsBuilder;
      final StringBuilder memCompFiltersBuilder;
      if (isAccount) {
        paramsBuilder.append("PublicKey _address,\n");
        if (hasDiscriminator) {
          paramsBuilder.append("Discriminator discriminator,\n");
          srcGenContext.addImport(Discriminator.class);
        }
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
            if (type.isFixedLength(srcGenContext)) {
              byteLength += type.serializedLength(srcGenContext);
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
            if (type.isFixedLength(srcGenContext)) {
              final int serializedLength = type.serializedLength(srcGenContext);
              if (serializedLength <= MAX_MEM_COMP_LENGTH) {
                field.generateMemCompFilter(srcGenContext, memCompFiltersBuilder, offsetVarName);
              }
              byteLength += serializedLength;
            } else {
              final int serializedLength = type.optimisticSerializedLength(srcGenContext);
              if (serializedLength > 0 && serializedLength <= MAX_MEM_COMP_LENGTH) {
                field.generateMemCompFilter(srcGenContext, memCompFiltersBuilder, offsetVarName);
              }
              byteLength = -1;
            }
          }
        }
        paramsBuilder.append(field.generateRecordField(srcGenContext));
        if (fieldIterator.hasNext()) {
          paramsBuilder.append(",\n");
        } else {
          break;
        }
      }

      final var params = paramsBuilder.toString().indent(recordSigLineLength).strip();
      builder
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
          srcGenContext.addImport(Filter.class);
        }

        final var accountDiscriminator = appendAccountDiscriminator(srcGenContext, builder, account);

        if (!offsetsBuilder.isEmpty()) {
          builder.append(offsetsBuilder.toString().indent(tabLength));
          builder.append(memCompFiltersBuilder.toString().indent(tabLength)).append('\n');
        }
      } else if (byteLength > 0) {
        builder.append('\n');
      }

      final var returnNewLine = String.format("return new %s(", name);
      if (hasStringFields()) {
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
          factoryMethodBuilder.append("final ").append(field.generateStaticFactoryField(srcGenContext));
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
          newInstanceBuilder.append(field.generateNewInstanceField(srcGenContext));
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
        readBuilder.append(field.generateRead(srcGenContext, hasNext, singleField, offsetVarName)).append('\n');
        if (!hasNext) {
          break;
        }
      }
      builder.append(String.format("public static %s read(final byte[] _data, final int offset) {", name).indent(tabLength));
      if (isAccount) {
        StructGen.readAccountInfo(srcGenContext, builder, name);
      }

      builder.append("""
          if (_data == null || _data.length == 0) {""".indent(tabLength << 1));
      builder.append(tab).append("""
          return null;
          }""".indent(tabLength << 1)
      );
      if (hasDiscriminator) {
        builder.append("""
            final var discriminator = createAnchorDiscriminator(_data, offset);
            int i = offset + discriminator.length();""".indent(tabLength << 1)
        );
        srcGenContext.addStaticImport(Discriminator.class, "createAnchorDiscriminator");
      } else if (!singleField) {
        builder.append(tab).append(tab).append("int i = offset;\n");
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
        newInstanceBuilder.append(field.generateNewInstanceField(srcGenContext));
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
        writeBuilder.append(field.generateWrite(srcGenContext, true)).append('\n');
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
        lengthBuilder.append(field.generateLength(srcGenContext));
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
  }

  String generatePublicRecord(final SrcGenContext srcGenContext,
                              final NamedType context,
                              final boolean isAccount,
                              final NamedType account,
                              final boolean maybeHasDiscriminator) {
    final boolean hasDiscriminator = srcGenContext.hasDiscriminator(isAccount, maybeHasDiscriminator);
    return generateRecord(srcGenContext, context, true, "Borsh", -1, isAccount, account, hasDiscriminator);
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
  public boolean isFixedLength(final SrcGenContext srcGenContext) {
    return fields.stream()
        .map(NamedType::type)
        .allMatch(type -> type.isFixedLength(srcGenContext));
  }

  @Override
  public int serializedLength(final SrcGenContext srcGenContext, final boolean isAccount) {
    final boolean hasDiscriminator = srcGenContext.hasDiscriminator(isAccount);
    int serializedLength = hasDiscriminator ? AnchorUtil.DISCRIMINATOR_LENGTH : 0;
    int len;
    for (final var field : fields) {
      final var type = field.type();
      len = field.type().serializedLength(srcGenContext, srcGenContext.isAccount(type.typeName()));
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
  public int optimisticSerializedLength(final SrcGenContext srcGenContext, final boolean isAccount) {
    final boolean hasDiscriminator = srcGenContext.hasDiscriminator(isAccount);
    int serializedLength = hasDiscriminator ? AnchorUtil.DISCRIMINATOR_LENGTH : 0;
    for (final var field : fields) {
      if (isFixedLength(srcGenContext)) {
        final var type = field.type();
        serializedLength += type.serializedLength(srcGenContext, srcGenContext.isAccount(type.typeName()));
      } else {
        return serializedLength;
      }
    }
    return serializedLength;
  }

  public String generateSource(final SrcGenContext srcGenContext, final NamedType context) {
    final var builder = new StringBuilder(4_096);
    srcGenContext.addImport(Borsh.class);
    final var recordSource = generatePublicRecord(srcGenContext, context, false, null, true);
    return builder.append('\n').append(recordSource).toString();
  }

  public String generateSource(final SrcGenContext srcGenContext,
                               final String packageName,
                               final NamedType context,
                               final boolean isAccount,
                               final NamedType account) {
    final var builder = new StringBuilder(4_096);
    builder.append("package ").append(packageName).append(";\n\n");

    srcGenContext.addImport(Borsh.class);

    final var recordSource = generatePublicRecord(srcGenContext, context, isAccount, account, isAccount);

    srcGenContext.appendImports(builder);

    return builder.append('\n').append(recordSource).toString();
  }
}
