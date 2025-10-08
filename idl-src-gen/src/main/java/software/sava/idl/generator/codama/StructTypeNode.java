package software.sava.idl.generator.codama;

import software.sava.anchor.AnchorUtil;
import software.sava.core.programs.Discriminator;
import software.sava.core.rpc.Filter;
import software.sava.idl.generator.anchor.SrcGenContext;
import software.sava.idl.generator.src.BaseStruct;
import software.sava.idl.generator.src.NamedType;
import software.sava.idl.generator.src.StructGen;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Locale.ENGLISH;
import static software.sava.core.rpc.Filter.MAX_MEM_COMP_LENGTH;
import static software.sava.idl.generator.src.SrcUtil.replaceNewLinesIfLessThan;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class StructTypeNode extends BaseStruct<StructFieldTypeNode> implements TypeNode {

  StructTypeNode(final List<StructFieldTypeNode> fields) {
    super(fields);
  }

  public String generateRecord(final SrcGenContext srcGenContext,
                               final NamedType namedNode,
                               final boolean publicAccess,
                               final String interfaceName) {
    final var tab = srcGenContext.tab();
    final var builder = new StringBuilder(4_096);
    namedNode.appendDocs(builder);

    final var name = namedNode.name();
    final int recordSigLineLength = StructGen.sigLine(builder, name, publicAccess);

    if (fields.isEmpty()) {
      return StructGen.emptyStruct(tab, builder, interfaceName, name);
    }

    final var paramsBuilder = new StringBuilder(4_096);
    final StringBuilder offsetsBuilder;
    final StringBuilder memCompFiltersBuilder;
    int byteLength = 0;
    final AccountNode accountNode = namedNode instanceof AccountNode account ? account : null;
    if (accountNode != null) {
      paramsBuilder.append("PublicKey _address,\n");
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
      offsetsBuilder = null;
      memCompFiltersBuilder = null;
      for (final var field : fields) {
        if (field.isFixedLength(srcGenContext)) {
          byteLength += field.type().serializedLength(srcGenContext);
        } else {
          byteLength = -1;
          break;
        }
      }
    }
    var fieldIterator = fields.iterator();
    for (StructFieldTypeNode field; ; ) {
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
    if (accountNode != null) {
      if (byteLength > 0) {
        builder.append(tab).append("""
            public static final Filter SIZE_FILTER = Filter.createDataSizeFilter(BYTES);
            
            """);
        srcGenContext.addImport(Filter.class);
      }
      for (final var discriminator : accountNode.discriminators()) {
        if (discriminator instanceof ConstantDiscriminatorNode(final ValueNode.Constant constant, final int offset)) {
          srcGenContext.addImport(Filter.class);
          srcGenContext.addImport(Discriminator.class);
          srcGenContext.addStaticImport(Discriminator.class, "toDiscriminator");

          if (constant.val() instanceof final ValueNode.Bytes bytes) {
            accountDiscriminator = bytes.createDiscriminator();
          } else {
            throw new UnsupportedOperationException("Add support for discriminator constant " + constant.val());
          }
          final var discriminatorLine = Arrays.stream(accountDiscriminator.toIntArray())
              .mapToObj(Integer::toString)
              .collect(Collectors.joining(", ",
                  "public static final Discriminator DISCRIMINATOR = toDiscriminator(", ");"
              ));
          if (offset > 0) {
            builder.append(tab).append("public static final int DISCRIMINATOR_OFFSET = ").append(offset).append(";\n");
          }
          builder.append(tab).append(discriminatorLine);
          builder.append("\n").append(tab).append(String.format("""
                  public static final Filter DISCRIMINATOR_FILTER = Filter.createMemCompFilter(%d, DISCRIMINATOR.data());
                  
                  """, offset
              )
          );
        } else {
          throw new UnsupportedOperationException("Add support for account " + discriminator);
        }
      }

      if (!offsetsBuilder.isEmpty()) {
        builder.append(offsetsBuilder.toString().indent(tabLength));
        builder.append(memCompFiltersBuilder.toString().indent(tabLength)).append('\n');
      }
    } else if (byteLength > 0) {
      builder.append('\n');
    }

    final var returnNewLine = String.format("return new %s(", name);
    // String fields require multiple lines to construct the record.
    if (fields.stream().anyMatch(field -> field.leafType() instanceof StringTypeNode)) {
      final var factoryMethodBuilder = new StringBuilder(2_048);
      if (accountNode != null) {
        factoryMethodBuilder.append("final PublicKey _address,\n");
      }
      if (accountDiscriminator != null) {
        factoryMethodBuilder.append("final Discriminator discriminator,\n");
      }
      fieldIterator = fields.iterator();
      for (StructFieldTypeNode field; ; ) {
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
      if (accountNode != null) {
        newInstanceBuilder.append("_address,\n");
        if (accountDiscriminator != null) {
          newInstanceBuilder.append("discriminator,\n");
        }
      }
      fieldIterator = fields.iterator();
      for (StructFieldTypeNode field; ; ) {
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
    for (StructFieldTypeNode field; ; ) {
      field = fieldIterator.next();
      final boolean hasNext = fieldIterator.hasNext();
      readBuilder.append(field.generateRead(srcGenContext, hasNext, singleField, offsetVarName)).append('\n');
      if (!hasNext) {
        break;
      }
    }
    builder.append(String.format("public static %s read(final byte[] _data, final int offset) {", name).indent(tabLength));
    if (accountNode != null) {
      StructGen.readAccountInfo(srcGenContext, builder, name);
    }

    return null;
  }

  static StructTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  private static final class Parser implements FieldBufferPredicate {

    private List<StructFieldTypeNode> fields;

    StructTypeNode createTypeNode() {
      return new StructTypeNode(fields);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("fields", buf, offset, len)) {
        fields = new ArrayList<>();
        while (ji.readArray()) {
          fields.add(StructFieldTypeNode.parse(ji));
        }
      } else {
        throw new IllegalStateException(String.format(
            "Unhandled %s field %s.",
            getClass().getSimpleName(), new String(buf, offset, len)
        ));
      }
      return true;
    }
  }
}
