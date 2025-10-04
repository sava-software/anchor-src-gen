package software.sava.idl.generator.codama;

import systems.comodal.jsoniter.JsonIterator;

import static systems.comodal.jsoniter.JsonIterator.fieldEquals;

final class EnumTupleVariantTypeNode extends OrdinalNode implements TypeNode, EnumVariantTypeNode {

  private final NestedTypeNode tuple;

  public EnumTupleVariantTypeNode(final String name, final int discriminator, final NestedTypeNode tuple) {
    super(name, discriminator);
    this.tuple = tuple;
  }

  public NestedTypeNode tuple() {
    return tuple;
  }

  public static EnumTupleVariantTypeNode parse(final JsonIterator ji) {
    final var parser = new Parser();
    ji.testObject(parser);
    return parser.createEnumTupleVariantTypeNode();
  }

  static final class Parser extends BaseParser {

    private int discriminator;
    private NestedTypeNode tuple;

    EnumTupleVariantTypeNode createEnumTupleVariantTypeNode() {
      return new EnumTupleVariantTypeNode(name, discriminator, tuple);
    }

    @Override
    public boolean test(final char[] buf, final int offset, final int len, final JsonIterator ji) {
      if (fieldEquals("discriminator", buf, offset, len)) {
        discriminator = ji.readInt();
        return true;
      } else if (fieldEquals("tuple", buf, offset, len)) {
        tuple = TypeNode.parseNestedTypeNode(ji, TupleTypeNode::parse);
        return true;
      } else {
        return super.test(buf, offset, len, ji);
      }
    }
  }
}
