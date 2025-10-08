package software.sava.idl.generator.codama;

import software.sava.anchor.AnchorUtil;
import software.sava.core.programs.Discriminator;
import software.sava.idl.generator.src.BaseNamedType;
import software.sava.idl.generator.src.NamedType;
import software.sava.idl.generator.src.SrcUtil;
import systems.comodal.jsoniter.JsonIterator;

import java.util.List;
import java.util.Objects;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class StructFieldTypeNode extends BaseNamedType<TypeNode> implements TypeNode {

  private final ValueNode defaultValue;
  private final ValueStrategy defaultValueStrategy;

  StructFieldTypeNode(final String name,
                      final List<String> docs,
                      final String docComments,
                      final TypeNode type,
                      final ValueNode defaultValue,
                      final ValueStrategy defaultValueStrategy) {
    super(name, type, docs, docComments);
    this.defaultValue = defaultValue;
    this.defaultValueStrategy = defaultValueStrategy;
  }

  TypeNode leafType() {
    var type = this.type;
    while (type instanceof NestedTypeNode nestedTypeNode) {
      type = nestedTypeNode.typeNode();
    }
    return type;
  }

  @Override
  public boolean isString() {
    return leafType().isString();
  }

  ValueNode defaultValue() {
    return defaultValue;
  }

  ValueStrategy defaultValueStrategy() {
    return defaultValueStrategy;
  }

  @Override
  public Discriminator discriminator() {
    throw new UnsupportedOperationException("TODO");
  }

  @Override
  public NamedType rename(final String newName) {
    return new StructFieldTypeNode(
        newName,
        docs,
        docComments,
        type,
        defaultValue,
        defaultValueStrategy
    );
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
          docs == null ? "" : SrcUtil.formatComments(docs),
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

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final StructFieldTypeNode that)) return false;
    if (!super.equals(o)) return false;
    return type.equals(that.type)
        && Objects.equals(defaultValue, that.defaultValue)
        && defaultValueStrategy == that.defaultValueStrategy;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + Objects.hashCode(defaultValue);
    result = 31 * result + Objects.hashCode(defaultValueStrategy);
    return result;
  }
}
