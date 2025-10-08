package software.sava.idl.generator.src;

import software.sava.anchor.AnchorUtil;
import software.sava.core.programs.Discriminator;
import software.sava.core.rpc.Filter;
import software.sava.idl.generator.anchor.SrcGenContext;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;
import static software.sava.core.rpc.Filter.MAX_MEM_COMP_LENGTH;
import static software.sava.idl.generator.ParseUtil.removeBlankLines;
import static software.sava.idl.generator.src.SrcUtil.replaceNewLinesIfLessThan;

public abstract class BaseStruct<F extends NamedType> implements IdlStruct {

  protected static final String LENGTH_ADD_ALIGN_TAB = " ".repeat("retur".length());

  protected final List<F> fields;

  protected BaseStruct(final List<F> fields) {
    this.fields = fields;
  }

  public final List<F> fields() {
    return fields;
  }

  protected final void appendAccountDiscriminator(final SrcGenContext srcGenContext,
                                                  final StringBuilder builder,
                                                  final Discriminator accountDiscriminator,
                                                  final int offset) {
    srcGenContext.addImport(Filter.class);
    srcGenContext.addImport(Discriminator.class);
    srcGenContext.addStaticImport(Discriminator.class, "toDiscriminator");

    final var tab = srcGenContext.tab();
    if (offset > 0) {
      builder.append(tab).append("public static final int DISCRIMINATOR_OFFSET = ").append(offset).append(";\n");
    }
    final var discriminatorLine = Arrays.stream(accountDiscriminator.toIntArray())
        .mapToObj(Integer::toString)
        .collect(Collectors.joining(", ",
            "public static final Discriminator DISCRIMINATOR = toDiscriminator(", ");"
        ));
    builder.append(tab).append(discriminatorLine);
    builder.append("\n").append(tab).append(String.format("""
            public static final Filter DISCRIMINATOR_FILTER = Filter.createMemCompFilter(%d, DISCRIMINATOR.data());
            
            """, offset
        )
    );
    if (offset != 0) {
      throw new UnsupportedOperationException("TODO: Support non-zero discriminator offsets: " + this);
    }
  }

  protected abstract boolean hasStringFields();

  protected abstract Discriminator appendAccountDiscriminator(final SrcGenContext srcGenContext,
                                                              final StringBuilder builder,
                                                              final NamedType account);

  protected String generateRecord(final SrcGenContext srcGenContext,
                                  final NamedType namedType,
                                  final boolean publicAccess,
                                  final String interfaceName,
                                  final int ordinal,
                                  final boolean isAccount,
                                  final NamedType account,
                                  final boolean maybeHasDiscriminator) {
    final boolean hasDiscriminator = srcGenContext.hasDiscriminator(isAccount, maybeHasDiscriminator);
    final var tab = srcGenContext.tab();
    final var builder = new StringBuilder(4_096);
    namedType.appendDocs(builder);

    final var name = namedType.name();
    final int recordSigLineLength = StructGen.sigLine(builder, name, publicAccess);

    if (fields.isEmpty()) {
      return StructGen.emptyStruct(tab, builder, interfaceName, name);
    }

    // TODO: get complex discriminator first.

    final var paramsBuilder = new StringBuilder(4_096);
    final StringBuilder offsetsBuilder;
    final StringBuilder memCompFiltersBuilder;
    int byteLength = 0;
    if (isAccount) {
      paramsBuilder.append("PublicKey _address,\n");
      if (hasDiscriminator) {
        paramsBuilder.append("Discriminator discriminator,\n");
        srcGenContext.addImport(Discriminator.class);
      }
      offsetsBuilder = new StringBuilder(2_048);
      memCompFiltersBuilder = new StringBuilder(4_096);
      for (final var field : fields) {
        final var offsetVarName = AnchorUtil.snakeCase(field.name()).toUpperCase(ENGLISH) + "_OFFSET";
        offsetsBuilder.append(String.format("""
                public static final int %s = %d;
                """,
            offsetVarName, byteLength
        ));
        final var type = field.type();
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
          break;
        }
      }
    } else {
      if (hasDiscriminator) {
        paramsBuilder.append("Discriminator discriminator,\n");
      }
      offsetsBuilder = null;
      memCompFiltersBuilder = null;
      for (final var field : fields) {
        final var type = field.type();
        if (type.isFixedLength(srcGenContext)) {
          byteLength += type.serializedLength(srcGenContext);
        } else {
          byteLength = -1;
          break;
        }
      }
    }
    var fieldIterator = fields.iterator();
    for (F field; ; ) {
      field = fieldIterator.next();
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

    Discriminator accountDiscriminator = null;
    final int tabLength = tab.length();
    if (isAccount) {
      if (byteLength > 0) {
        builder.append(tab).append("""
            public static final Filter SIZE_FILTER = Filter.createDataSizeFilter(BYTES);
            
            """);
        srcGenContext.addImport(Filter.class);
      }
      accountDiscriminator = appendAccountDiscriminator(srcGenContext, builder, account);

      if (!offsetsBuilder.isEmpty()) {
        builder.append(offsetsBuilder.toString().indent(tabLength));
        builder.append(memCompFiltersBuilder.toString().indent(tabLength)).append('\n');
      }
    } else if (byteLength > 0) {
      builder.append('\n');
    }

    final var returnNewLine = String.format("return new %s(", name);
    // For String fields we keep both the byte[] and create a String.
    // A factory constructor is added here for convenience.
    if (hasStringFields()) {
      final var factoryMethodBuilder = new StringBuilder(2_048);
      if (isAccount) {
        factoryMethodBuilder.append("final PublicKey _address,\n");
        if (accountDiscriminator != null) {
          factoryMethodBuilder.append("final Discriminator discriminator,\n");
        }
      }
      fieldIterator = fields.iterator();
      for (F field; ; ) {
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
      if (accountDiscriminator != null) {
        newInstanceBuilder.append("discriminator,\n");
      }
      fieldIterator = fields.iterator();
      for (F field; ; ) {
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
    final boolean singleField = accountDiscriminator == null && fields.size() == 1;
    final var offsetVarName = singleField ? "offset" : "i";
    fieldIterator = fields.iterator();
    for (F field; ; ) {
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
    if (accountDiscriminator != null) {
      // TODO: Support non-zero offsets
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
    if (accountDiscriminator != null) {
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
      if (accountDiscriminator != null) {
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
    for (F field; ; ) {
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
        if (accountDiscriminator != null) {
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
          %sreturn %d;
          }
          """, tab, ordinal
      ).indent(tabLength));
    }

    return removeBlankLines(builder.append('}').toString());
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final BaseStruct<?> that)) return false;
    return fields.equals(that.fields);
  }

  @Override
  public int hashCode() {
    return fields.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" + "fields=" + fields + '}';
  }
}
