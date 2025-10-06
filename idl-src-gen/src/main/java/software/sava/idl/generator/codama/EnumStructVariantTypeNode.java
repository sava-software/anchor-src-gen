package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class EnumStructVariantTypeNode extends OrdinalNode implements TypeNode, EnumVariantTypeNode, StructNode {

  private final TypeNode struct;

  EnumStructVariantTypeNode(final String name, final int discriminator, final TypeNode struct) {
    super(name, discriminator);
    this.struct = struct;
  }

  TypeNode struct() {
    return struct;
  }

  static EnumStructVariantTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createEnumStructVariantTypeNode();
  }

  private static final class Parser extends BaseParser {

    private int discriminator;
    private TypeNode struct;

    EnumStructVariantTypeNode createEnumStructVariantTypeNode() {
      return new EnumStructVariantTypeNode(name, discriminator, struct);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("discriminator", buf, offset, len)) {
        discriminator = ji.readInt();
        return true;
      } else if (fieldEquals("struct", buf, offset, len)) {
        struct = TypeNode.parse(ji, StructTypeNode::parse);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
