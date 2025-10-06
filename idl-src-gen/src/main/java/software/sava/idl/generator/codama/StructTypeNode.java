package software.sava.idl.generator.codama;

import software.sava.anchor.AnchorUtil;
import software.sava.core.rpc.Filter;
import software.sava.idl.generator.src.StructGen;
import systems.comodal.jsoniter.FieldBufferPredicate;
import systems.comodal.jsoniter.JsonIterator;

import java.util.ArrayList;
import java.util.List;

import static java.util.Locale.ENGLISH;
import static software.sava.core.rpc.Filter.MAX_MEM_COMP_LENGTH;
import static software.sava.idl.generator.src.SrcUtil.replaceNewLinesIfLessThan;
import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

record StructTypeNode(List<StructFieldTypeNode> fields) implements TypeNode {

  static StructTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createTypeNode();
  }

  public String generateRecord(final SrcGenContext srcGenContext,
                               final NamedNode namedNode,
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
    final boolean isAccount = namedNode instanceof AccountNode;
    if (isAccount) {
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

    final int tabLength = tab.length();
    if (isAccount) {
      if (byteLength > 0) {
        builder.append(tab).append("""
            public static final Filter SIZE_FILTER = Filter.createDataSizeFilter(BYTES);
            
            """);
        srcGenContext.addImport(Filter.class);
      }
// TODO: Check if the account has a constant discriminator.
//      final var discriminator = account.discriminator();
//      if (discriminator != null) {
//        srcGenContext.addImport(Filter.class);
//        srcGenContext.addImport(Discriminator.class);
//        srcGenContext.addStaticImport(Discriminator.class, "toDiscriminator");
//
//        final var discriminatorLine = Arrays.stream(discriminator.toIntArray())
//            .mapToObj(Integer::toString)
//            .collect(Collectors.joining(", ",
//                "public static final Discriminator DISCRIMINATOR = toDiscriminator(", ");"
//            ));
//        builder.append(tab).append(discriminatorLine);
//        builder.append("\n").append(tab).append("""
//            public static final Filter DISCRIMINATOR_FILTER = Filter.createMemCompFilter(0, DISCRIMINATOR.data());
//
//            """);
//      }

      if (!offsetsBuilder.isEmpty()) {
        builder.append(offsetsBuilder.toString().indent(tabLength));
        builder.append(memCompFiltersBuilder.toString().indent(tabLength)).append('\n');
      }
    } else if (byteLength > 0) {
      builder.append('\n');
    }

    return null;
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
