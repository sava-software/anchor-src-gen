package software.sava.idl.generator.codama;

import software.sava.anchor.AnchorUtil;
import systems.comodal.jsoniter.JsonIterator;

import java.util.List;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class StructFieldTypeNode extends NamedDocsNode {

  private final TypeNode type;
  private final ValueNode defaultValue;
  private final ValueStrategy defaultValueStrategy;

  StructFieldTypeNode(final String name,
                      final List<String> docs,
                      final TypeNode type,
                      final ValueNode defaultValue,
                      final ValueStrategy defaultValueStrategy) {
    super(name, docs);
    this.type = type;
    this.defaultValue = defaultValue;
    this.defaultValueStrategy = defaultValueStrategy;
  }

  TypeNode type() {
    return type;
  }

  ValueNode defaultValue() {
    return defaultValue;
  }

  ValueStrategy defaultValueStrategy() {
    return defaultValueStrategy;
  }

  void generateMemCompFilter(final SrcGenContext srcGenContext,
                             final StringBuilder builder,
                             final String offsetVarName) {
    type.generateMemCompFilter(srcGenContext, builder, name, offsetVarName);
  }

  String generateRecordField(final SrcGenContext srcGenContext) {
    return type.generateRecordField(srcGenContext, this, false);
  }

  public String arrayLengthConstant() {
    if (type instanceof ArrayTypeNode(_, CountNode count)) {
      final int length;
      if (count instanceof FixedCountNode(int value)) {
        length = value;
      } else if (count instanceof PrefixedCountNode(TypeNode prefix)) {
        if (prefix instanceof FixedSizeTypeNode fixedSizeTypeNode) {
          length = fixedSizeTypeNode.size();
        } else {
          return null;
        }
      } else {
        return null;
      }
      return String.format("public static final int %s_LEN = %d;\n", AnchorUtil.snakeCase(name, true), length);
    } else {
      return null;
    }
  }

  static StructFieldTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createStructFieldTypeNode();
  }

  private static final class Parser extends BaseDocsParser {

    private TypeNode type;
    private ValueNode defaultValue;
    private ValueStrategy defaultValueStrategy;

    private Parser() {
    }

    StructFieldTypeNode createStructFieldTypeNode() {
      return new StructFieldTypeNode(
          name,
          docs == null ? List.of() : docs,
          type,
          defaultValue,
          defaultValueStrategy
      );
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("type", buf, offset, len)) {
        type = TypeNode.parse(ji);
      } else if (fieldEquals("defaultValue", buf, offset, len)) {
        defaultValue = ValueNode.parse(ji);
      } else if (fieldEquals("defaultValueStrategy", buf, offset, len)) {
        defaultValueStrategy = ValueStrategy.valueOf(ji.readString());
      } else {
        return super.test(buf, offset, len, ji);
      }
      return true;
    }
  }
}
